package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.collection.AutoCollectionOutcome
import com.soma369.laimory.core.domain.model.collection.AutoCollectionResult
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.provider.CollectionAvailabilityProvider
import com.soma369.laimory.core.domain.usecase.CollectCalendarUseCase
import com.soma369.laimory.core.domain.usecase.CollectHealthUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AutoCollectionCoordinator] 기본 구현.
 *
 * 작업을 애플리케이션 scope 의 [Deferred] 하나로 들고 있어, 여러 호출자가 같은 수집을 공유하고
 * 호출자가 취소돼도 수집은 이어진다 — `await` 취소는 그 호출자만 떠나게 한다.
 *
 * 유형별 마지막 **성공** 시각을 따로 둔다. 캘린더는 성공하고 건강은 실패한 경우 건강만 다시
 * 시도해야 하는데, 완료 시각을 하나로 두면 둘 다 5분간 묶이거나 둘 다 매번 다시 긁는다.
 *
 * 성공이 아닌 결과는 캐시하지 않는다 — 권한을 켜고 돌아온 사용자가 창이 닫힐 때까지 기다리게 된다.
 *
 * 세션 세대([sessionEpoch])를 두어 인증 경계가 바뀌면 이전 세션의 작업 결과가 새 세션의 최신성
 * 상태로 반영되지 않게 한다.
 */
@Singleton
class DefaultAutoCollectionCoordinator
    @Inject
    constructor(
        private val availability: CollectionAvailabilityProvider,
        private val collectCalendar: CollectCalendarUseCase,
        private val collectHealth: CollectHealthUseCase,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) : AutoCollectionCoordinator {
        private val mutex = Mutex()
        private var runningJob: Deferred<Map<ItemType, AutoCollectionOutcome>>? = null
        private var runningEpoch = 0L

        /**
         * 유형별 마지막 성공. **세대를 함께 들고 있는다.**
         *
         * 세대가 다르면 값이 남아 있어도 없는 셈으로 본다. 그래서 세션이 바뀔 때 지도를 비우는
         * 별도 정리가 필요 없다 — 세대만 올리면 이전 세션의 모든 캐시가 그 자리에서 무효가 된다.
         * 비우기를 코루틴으로 미루면 그 코루틴이 자물쇠를 잡기 전에 새 세션의 [refresh] 가 이전
         * 값을 보고 수집을 건너뛴다.
         */
        private val lastSucceeded = mutableMapOf<ItemType, Freshness>()

        /** 인증 세션 세대. [discard] 가 **호출 즉시** 올린다. */
        @Volatile
        private var sessionEpoch = 0L

        override suspend fun refresh(timeoutMillis: Long?): AutoCollectionResult {
            // 다시 볼 유형이 없으면 곧장 통과한다. 상한 초과와 달리 알릴 일이 아니다.
            val job = startIfNeeded() ?: return AutoCollectionResult()
            if (timeoutMillis == null) return AutoCollectionResult(outcomes = job.await())
            // 상한을 넘기면 기다림만 포기한다. 수집 자체는 계속 돌아 다음 호출이 결과를 쓴다.
            val outcomes = withTimeoutOrNull(timeoutMillis) { job.await() }
            return outcomes?.let { AutoCollectionResult(outcomes = it) } ?: AutoCollectionResult(timedOut = true)
        }

        override fun discard() {
            // 세대를 먼저 올린다. 이 한 줄로 이전 세션의 최신성 캐시가 그 자리에서 무효가 되므로
            // 새 세션의 refresh 가 곧바로 들어와도 옛 값을 보고 건너뛸 수 없다.
            val invalidated = sessionEpoch
            sessionEpoch = invalidated + 1
            applicationScope.launch {
                mutex.withLock {
                    // 그 사이 새 세션이 시작한 작업은 건드리지 않는다. 세대를 확인하지 않으면
                    // 뒤늦은 정리가 방금 시작한 새 작업을 취소한다.
                    if (runningEpoch != invalidated) return@withLock
                    runningJob?.cancel()
                    runningJob = null
                }
            }
        }

        /**
         * 다시 볼 유형이 있으면 작업을 만들고, 이미 돌고 있으면 그 작업을 돌려준다.
         * 모든 유형이 최근에 끝났으면 null 이다.
         */
        private suspend fun startIfNeeded(): Deferred<Map<ItemType, AutoCollectionOutcome>>? =
            mutex.withLock {
                val epoch = sessionEpoch
                // 이전 세션의 작업은 재사용하지 않는다. 계정이 바뀌었는데 그 결과를 물려받으면 안 된다.
                runningJob?.takeIf { it.isActive && runningEpoch == epoch }?.let { return@withLock it }
                val stale = AUTO_COLLECTED_TYPES.filter { it.isStale(epoch) }
                if (stale.isEmpty()) return@withLock null
                applicationScope.async { collect(stale, epoch) }.also {
                    runningJob = it
                    runningEpoch = epoch
                }
            }

        private fun ItemType.isStale(epoch: Long): Boolean {
            val last = lastSucceeded[this] ?: return true
            // 이전 세션에서 성공한 값은 남아 있어도 없는 셈이다.
            if (last.epoch != epoch) return true
            return Duration.between(last.at, Instant.now()) >= FRESHNESS_WINDOW
        }

        private suspend fun collect(
            types: List<ItemType>,
            epoch: Long,
        ): Map<ItemType, AutoCollectionOutcome> {
            val outcomes =
                types.associateWith { type ->
                    when (type) {
                        ItemType.CALENDAR -> calendarOutcome()
                        ItemType.HEALTH -> healthOutcome()
                        else -> AutoCollectionOutcome.Failed
                    }
                }
            mutex.withLock {
                // 시작한 세션이 이미 끝났으면 결과를 최신성 상태에 반영하지 않는다.
                if (epoch != sessionEpoch) return outcomes
                // 성공만 캐시한다. 권한 없음·미지원은 다음 호출에서 곧장 다시 본다.
                val now = Instant.now()
                outcomes.forEach { (type, outcome) ->
                    if (outcome.isCacheable) lastSucceeded[type] = Freshness(epoch, now) else lastSucceeded.remove(type)
                }
                if (runningEpoch == epoch) runningJob = null
            }
            return outcomes
        }

        private suspend fun calendarOutcome(): AutoCollectionOutcome =
            runOutcome {
                if (!availability.canCollectCalendar()) {
                    AutoCollectionOutcome.PermissionDenied
                } else {
                    AutoCollectionOutcome.Collected(collectCalendar())
                }
            }

        private suspend fun healthOutcome(): AutoCollectionOutcome =
            runOutcome {
                when {
                    !availability.isHealthConnectAvailable() -> AutoCollectionOutcome.Unavailable
                    !availability.canCollectHealth() -> AutoCollectionOutcome.PermissionDenied
                    else -> AutoCollectionOutcome.Collected(collectHealth())
                }
            }

        /** 예외는 유형 하나만 실패로 떨어뜨린다. 로그에는 수집 원문이 실릴 수 있는 메시지를 남기지 않는다. */
        private suspend fun runOutcome(block: suspend () -> AutoCollectionOutcome): AutoCollectionOutcome =
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 예외 메시지에 일정 제목·건강 값이 실릴 수 있어 값으로만 올리고 여기서 기록하지 않는다.
                AutoCollectionOutcome.Failed
            }

        /** 마지막 성공 시각과 그때의 세션 세대. 세대가 다르면 재사용하지 않는다. */
        private data class Freshness(
            val epoch: Long,
            val at: Instant,
        )

        private companion object {
            val AUTO_COLLECTED_TYPES = listOf(ItemType.CALENDAR, ItemType.HEALTH)

            /** 이 시간 안에 결과가 나온 유형은 다시 긁지 않는다. */
            val FRESHNESS_WINDOW: Duration = Duration.ofMinutes(5)
        }
    }
