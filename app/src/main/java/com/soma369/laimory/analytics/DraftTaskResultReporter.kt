package com.soma369.laimory.analytics

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsCreateResult
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKeys
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsFailureCode
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 접수된 생성 작업의 끝을 `timeline_create_result` 로 기록한다.
 *
 * 코디네이터를 고치지 않고 **바깥에서 상태를 관찰한다.** 도메인이 분석을 알면 생성 흐름을 바꿀 때마다
 * 분석 코드가 끼어든다. 끝 상태(성공·실패·결과 없음)는 이미 [DraftTaskCoordinator.state] 로 나와 있다.
 *
 * 작업마다 한 번만 보낸다 — 같은 끝을 푸시와 폴링이 함께 관찰하고, 앱을 다시 켜면 저장된 활성 작업이
 * 복원되면서 같은 끝 상태가 다시 흐른다.
 */
@Singleton
class DraftTaskResultReporter
    @Inject
    constructor(
        private val coordinator: DraftTaskCoordinator,
        private val analyticsHelper: AnalyticsHelper,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) {
        private var job: Job? = null

        fun start() {
            if (job?.isActive == true) return
            job = applicationScope.launch { coordinator.state.collect(::report) }
        }

        private suspend fun report(state: DraftTaskTrackingState) {
            val (taskId, event) = state.toCreateResult() ?: return
            analyticsHelper.logOnce(AnalyticsDedupeKeys.timelineCreateResult(taskId), event)
        }
    }

/** 끝 상태면 작업 ID 와 결과 이벤트, 아니면 null. 진행 중·재시도 가능 오류는 끝이 아니다. */
internal fun DraftTaskTrackingState.toCreateResult(): Pair<String, AnalyticsEvent.TimelineCreateResult>? {
    val event =
        when (this) {
            is DraftTaskTrackingState.Success ->
                AnalyticsEvent.TimelineCreateResult(
                    result = AnalyticsCreateResult.SUCCESS,
                    recordDate = task.recordDate,
                    eventCount = eventCount,
                )
            is DraftTaskTrackingState.Failed ->
                AnalyticsEvent.TimelineCreateResult(AnalyticsCreateResult.FAILURE, AnalyticsFailureCode.from(reason))
            is DraftTaskTrackingState.Unavailable ->
                AnalyticsEvent.TimelineCreateResult(AnalyticsCreateResult.FAILURE, AnalyticsFailureCode.RESULT_UNAVAILABLE)
            is DraftTaskTrackingState.Processing,
            is DraftTaskTrackingState.LongRunning,
            is DraftTaskTrackingState.RetryableError,
            DraftTaskTrackingState.Idle,
            -> return null
        }
    return (this as DraftTaskTrackingState.WithTask).task.taskId to event
}
