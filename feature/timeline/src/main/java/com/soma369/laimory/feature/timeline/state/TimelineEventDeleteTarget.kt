package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable

/**
 * 카드 `⋮` 에서 삭제를 고른 Event.
 *
 * 하루 기록 삭제([TimelineRecordDeleteTarget])와 문구도 되돌릴 대상도 달라 상태를 따로 둔다.
 */
@Immutable
data class TimelineEventDeleteTarget(
    val timelineEventId: Long,
    val title: String,
)
