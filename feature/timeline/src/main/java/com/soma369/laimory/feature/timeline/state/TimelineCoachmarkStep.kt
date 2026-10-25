package com.soma369.laimory.feature.timeline.state

/**
 * 타임라인 첫 도착 코치마크의 단계. 선언 순서대로 짚는다.
 *
 * 처음 만든 초안에 도착하면 첫 단계부터 시작한다. 마지막 단계는 저장하기다 — 감정은 저장할 때 고르므로 그 단계에서 함께 알린다.
 */
enum class TimelineCoachmarkStep(
    val message: String,
) {
    EVENT("AI가 모은 기록으로 만든 하루예요. 이벤트를 눌러 시각·장소·설명을 고칠 수 있어요."),
    MEMO("AI 질문에 답하거나 그때의 이야기를 메모로 남겨 보세요."),
    ADD_EVENT("빠진 일이 있으면 여기서 직접 더할 수 있어요."),
    SAVE("다 고쳤으면 저장하기를 눌러요. 오늘의 감정을 고르면 하루 기록이 완성돼요."),
}
