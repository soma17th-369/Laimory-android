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
 * 유형별 마지막 결과 시각을 따로 둔다. 캘린더는 성공하고 건강은 실패한 경우 건강만 다시
 * 시도해야 하는데, 완료 시각을 하나로 두면 둘 다 5분간 묶이거나 둘 다 매번 다시 긁는다.
 *
 * 권한 없음·미지원도 시각을 갱신한다. 재시도해도 같은 답이라 5분마다 다시 물어볼 이유가 없다.
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
        private val lastResolvedAt = mutableMapOf<ItemType, Instant>()

        override suspend fun refresh(timeoutMillis: Long?): AutoCollectionResult {
            // 다시 볼 유형이 없으면 곧장 통과한다. 상한 초과와 달리 알릴 일이 아니다.
            val job = startIfNeeded() ?: return AutoCollectionResult()
            if (timeoutMillis == null) return AutoCollectionResult(outcomes = job.await())
            // 상한을 넘기면 기다림만 포기한다. 수집 자체는 계속 돌아 다음 호출이 결과를 쓴다.
            val outcomes = withTimeoutOrNull(timeoutMillis) { job.await() }
            return outcomes?.let { AutoCollectionResult(outcomes = it) } ?: AutoCollectionResult(timedOut = true)
        }

        override fun discard() {
            applicationScope.launch {
                mutex.withLock {
                    runningJob?.cancel()
                    runningJob = null
                    lastResolvedAt.clear()
                }
            }
        }

        /**
         * 다시 볼 유형이 있으면 작업을 만들고, 이미 돌고 있으면 그 작업을 돌려준다.
         * 모든 유형이 최근에 끝났으면 null 이다.
         */
        private suspend fun startIfNeeded(): Deferred<Map<ItemType, AutoCollectionOutcome>>? =
            mutex.withLock {
                runningJob?.takeIf { it.isActive }?.let { return@withLock it }
                val stale = AUTO_COLLECTED_TYPES.filter { it.isStale() }
                if (stale.isEmpty()) return@withLock null
                applicationScope.async { collect(stale) }.also { runningJob = it }
            }

        private fun ItemType.isStale(): Boolean {
            val last = lastResolvedAt[this] ?: return true
            return Duration.between(last, Instant.now()) >= FRESHNESS_WINDOW
        }

        private suspend fun collect(types: List<ItemType>): Map<ItemType, AutoCollectionOutcome> {
            val outcomes =
                types.associateWith { type ->
                    when (type) {
                        ItemType.CALENDAR -> calendarOutcome()
                        ItemType.HEALTH -> healthOutcome()
                        else -> AutoCollectionOutcome.Failed
                    }
                }
            mutex.withLock {
                // 실패한 유형만 시각을 비워 다음 호출에서 곧장 다시 시도하게 한다.
                outcomes.forEach { (type, outcome) ->
                    if (outcome.isRetryable) lastResolvedAt.remove(type) else lastResolvedAt[type] = Instant.now()
                }
                runningJob = null
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

        private companion object {
            val AUTO_COLLECTED_TYPES = listOf(ItemType.CALENDAR, ItemType.HEALTH)

            /** 이 시간 안에 결과가 나온 유형은 다시 긁지 않는다. */
            val FRESHNESS_WINDOW: Duration = Duration.ofMinutes(5)
        }
    }
