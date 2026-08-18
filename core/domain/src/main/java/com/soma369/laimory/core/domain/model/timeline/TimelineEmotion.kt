package com.soma369.laimory.core.domain.model.timeline

/** 하루 전체를 대표하는 감정. 서버에 새 literal이 추가되면 [UNKNOWN]으로 안전하게 표시한다. */
enum class TimelineEmotion {
    VERY_HAPPY,
    HAPPY,
    NEUTRAL,
    UNHAPPY,
    VERY_UNHAPPY,
    UNKNOWN,
    ;

    companion object {
        /**
         * 사용자가 직접 고를 수 있는 감정. 표시 순서도 이 순서를 정본으로 삼는다.
         *
         * [UNKNOWN]은 조회에서 모르는 literal을 수렴시키는 표시 상태라 선택지에 넣지 않는다 —
         * 저장 요청에 실리면 서버가 `400/-400`으로 거절한다.
         */
        val SELECTABLE: List<TimelineEmotion> = listOf(VERY_HAPPY, HAPPY, NEUTRAL, UNHAPPY, VERY_UNHAPPY)

        /** 감정을 고르지 않은 사용자에게 기본으로 제시하는 감정(무덤덤). */
        val DEFAULT_SELECTION: TimelineEmotion = NEUTRAL
    }
}
