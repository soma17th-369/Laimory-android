package com.soma369.laimory.core.domain.model.timeline

/** 하루 전체를 대표하는 감정. 서버에 새 literal이 추가되면 [UNKNOWN]으로 안전하게 표시한다. */
enum class TimelineEmotion {
    VERY_HAPPY,
    HAPPY,
    NEUTRAL,
    UNHAPPY,
    VERY_UNHAPPY,
    UNKNOWN,
}
