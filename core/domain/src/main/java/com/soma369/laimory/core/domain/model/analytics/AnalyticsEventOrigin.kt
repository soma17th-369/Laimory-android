package com.soma369.laimory.core.domain.model.analytics

/**
 * 타임라인 이벤트를 누가 만들었는지. 완료 요약에서 AI 가 만든 것과 사용자가 직접 추가한 것을 나눈다.
 *
 * 서버 응답에 출처 필드가 없어 AI 질문(`question`) 유무로 가른다. AI 는 draft 의 모든 이벤트에 질문을
 * 채우고(비면 한 번 더 묻는다), 서버의 수동 생성은 질문을 항상 비운다. 그래서 질문이 있으면 AI,
 * 없으면 직접 추가로 본다 — AI 질문 생성이 두 번 연달아 실패한 이벤트만 직접 추가로 잘못 센다.
 *
 * 나눠 세는 이유: 직접 추가한 이벤트는 만들면서 메모를 같이 쓰는 경우가 많아, 합치면 AI 이벤트에
 * 사용자가 얼마나 덧붙였는지가 흐려진다.
 */
enum class AnalyticsEventOrigin {
    AI,
    MANUAL,
    ;

    companion object {
        fun of(question: String?): AnalyticsEventOrigin = if (question.isNullOrBlank()) MANUAL else AI
    }
}
