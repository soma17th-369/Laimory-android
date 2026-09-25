package com.soma369.laimory.core.domain.model.analytics

/**
 * 기록을 완료하기 전까지 사용자가 이벤트를 고치고 지운 흔적. 완료 이벤트의 요약 건수로만 쓴다.
 *
 * 서버는 AI 가 만든 원본을 남기지 않아(편집이 같은 행을 덮어씀) 완료 순간에는 무엇이 고쳐졌는지 알 수
 * 없다. 그래서 편집이 성공할 때마다 기기에 표시해 둔다. 기기에만 남으므로 다른 기기에서 고친 것은
 * 세지 못한다.
 *
 * @property editedEventIds 메모가 아닌 내용(종류·제목·설명·시각·사진)을 고친 이벤트.
 * @property deletedAiEventIds 지운 AI 이벤트. 완료 순간에는 목록에 없어 출처를 알 수 없으므로 지울 때 가려 둔다.
 */
data class AnalyticsTimelineEditLog(
    val editedEventIds: Set<Long>,
    val deletedAiEventIds: Set<Long>,
) {
    companion object {
        val EMPTY = AnalyticsTimelineEditLog(editedEventIds = emptySet(), deletedAiEventIds = emptySet())
    }
}
