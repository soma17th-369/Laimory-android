package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.model.analytics.AnalyticsEditorMode
import com.soma369.laimory.core.domain.model.analytics.AnalyticsTimelineEventTarget
import java.time.LocalDate

/**
 * 편집 화면 한 번의 방문. 열림과 이탈 이벤트를 같은 값으로 잇는다.
 *
 * @property timelineEventId 고치러 연 사건. 새 사건이면 `null`.
 * @property target 열 때 잡아 둔 사건 정보. 지우고 나간 뒤에는 세션에서 빠져 다시 구할 수 없다.
 * @property openedAtMillis 폼이 보인 시각. 머문 시간을 잰다.
 */
internal data class TimelineEventEditorVisit(
    val mode: AnalyticsEditorMode,
    val timelineEventId: Long?,
    val target: AnalyticsTimelineEventTarget?,
    val recordDate: LocalDate,
    val openedAtMillis: Long,
)
