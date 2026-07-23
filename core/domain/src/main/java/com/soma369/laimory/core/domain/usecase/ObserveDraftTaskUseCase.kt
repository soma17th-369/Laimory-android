package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.timeline.DraftPollingPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftTaskPollingEvent
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatus
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatusOutcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * 한 초안 작업을 5초 간격으로 조회하고 terminal·오류·장기 처리 경계에서 관찰을 끝낸다.
 *
 * 앱 수명주기에 따른 시작·중지는 상위 coordinator 책임이다. `계속 대기`를 선택한 뒤에는
 * [pauseAtLongRunning]을 false로 호출해 같은 작업을 terminal까지 계속 관찰할 수 있다.
 */
class ObserveDraftTaskUseCase
    @Inject
    constructor(
        private val getDraftTaskStatusUseCase: GetDraftTaskStatusUseCase,
        private val policy: DraftPollingPolicy,
        private val clock: Clock,
    ) {
        operator fun invoke(
            taskId: String,
            requestedAt: Instant,
            pauseAtLongRunning: Boolean = true,
        ): Flow<DraftTaskPollingEvent> =
            flow {
                while (true) {
                    val result = getDraftTaskStatusUseCase(taskId)
                    val outcome =
                        result.getOrElse { cause ->
                            emit(DraftTaskPollingEvent.RetryableFailure(cause))
                            return@flow
                        }
                    emit(DraftTaskPollingEvent.Status(outcome))

                    val snapshot = (outcome as? DraftTaskStatusOutcome.Snapshot)?.value
                    if (snapshot?.status != DraftTaskStatus.PROCESSING) return@flow

                    val elapsedSeconds =
                        maxOf(
                            snapshot.elapsedSeconds ?: 0L,
                            Duration.between(requestedAt, clock.instant()).seconds.coerceAtLeast(0L),
                        )
                    if (pauseAtLongRunning && elapsedSeconds >= policy.longRunningSeconds) {
                        emit(DraftTaskPollingEvent.LongRunning(elapsedSeconds))
                        return@flow
                    }
                    delay(policy.intervalMillis)
                }
            }
    }
