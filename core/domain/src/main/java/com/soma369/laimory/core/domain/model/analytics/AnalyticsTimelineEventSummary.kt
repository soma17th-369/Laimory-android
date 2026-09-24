package com.soma369.laimory.core.domain.model.analytics

/**
 * 기록을 완료한 순간의 이벤트 요약. `timeline_completed` 에 싣는다.
 *
 * 메모·수정·삭제를 할 때마다 보내지 않고 완료 시점의 최종 상태로 본다 — 스펙이 개별 편집 행동을
 * 핵심 이벤트에서 빼고 "완료 시점 요약" 으로 AI 품질을 보기로 한 것과 같은 방향이다.
 *
 * AI 가 만든 이벤트와 사용자가 직접 추가한 이벤트를 나눈다([AnalyticsEventOrigin]). 수정·삭제는
 * AI 이벤트만 센다 — AI 결과를 사용자가 얼마나 고쳐 받아들였는지가 알고 싶은 것이다. 직접 추가한
 * 이벤트 수는 [manualEventCount] 가 곧 추가 건수다.
 *
 * 메모 있음 = 공백 제외 1자 이상.
 */
data class AnalyticsTimelineEventSummary(
    val aiEventCount: Int,
    val aiMemoEventCount: Int,
    val aiEditedEventCount: Int,
    val aiDeletedEventCount: Int,
    val manualEventCount: Int,
    val manualMemoEventCount: Int,
) {
    companion object {
        fun of(
            events: List<AnalyticsTimelineEventSnapshot>,
            editLog: AnalyticsTimelineEditLog,
        ): AnalyticsTimelineEventSummary {
            val (ai, manual) = events.partition { event -> AnalyticsEventOrigin.of(event.question) == AnalyticsEventOrigin.AI }
            return AnalyticsTimelineEventSummary(
                aiEventCount = ai.size,
                aiMemoEventCount = ai.count { event -> event.hasMemo() },
                // 고친 뒤 지운 이벤트는 목록에 없어 여기서 빠지고 삭제로만 센다.
                aiEditedEventCount = ai.count { event -> event.timelineEventId in editLog.editedEventIds },
                aiDeletedEventCount = editLog.deletedAiEventIds.size,
                manualEventCount = manual.size,
                manualMemoEventCount = manual.count { event -> event.hasMemo() },
            )
        }

        private fun AnalyticsTimelineEventSnapshot.hasMemo(): Boolean = !memo.isNullOrBlank()
    }
}
