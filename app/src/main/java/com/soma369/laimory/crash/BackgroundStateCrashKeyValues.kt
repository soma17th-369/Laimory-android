package com.soma369.laimory.crash

import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState

/**
 * 위치 수집 꼬리표 값. 사용자가 켜 뒀어도 권한이 없으면 수집은 돌지 않으므로 둘을 함께 본다.
 *
 * 의사만 남기면 "켜 뒀는데 위치를 못 받는" 경우가 `on` 으로 보여, 리포트만으로는 가려지지 않는다.
 */
internal fun locationTrackingKeyValue(
    enabled: Boolean,
    canTrack: Boolean,
): String =
    when {
        !enabled -> "off"
        !canTrack -> "no_permission"
        else -> "on"
    }

internal fun sleepDetectionKeyValue(enabled: Boolean): String = if (enabled) "on" else "off"

internal fun notificationAccessKeyValue(granted: Boolean): String = if (granted) "granted" else "denied"

/** 초안 작업 꼬리표 값. 단계만 남긴다 — 작업 식별자·기록 날짜·경과 시간은 넣지 않는다. */
internal fun draftTaskKeyValue(state: DraftTaskTrackingState): String =
    when (state) {
        DraftTaskTrackingState.Idle -> "idle"
        is DraftTaskTrackingState.Processing -> "processing"
        is DraftTaskTrackingState.LongRunning -> "long_running"
        is DraftTaskTrackingState.Success -> "success"
        is DraftTaskTrackingState.Failed -> "failed"
        is DraftTaskTrackingState.RetryableError -> "retryable_error"
        is DraftTaskTrackingState.Unavailable -> "unavailable"
    }
