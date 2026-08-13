package com.soma369.laimory.core.ui.model

import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.ui.theme.Emotion

/**
 * 서버 감정 literal 을 표시 팔레트로 옮긴다.
 *
 * 홈·캘린더가 같은 매핑을 써야 같은 기록이 같은 색으로 보이므로 feature 모듈이 각자 갖지 않고
 * core:ui 의 공통 경계에 둔다. 감정을 알 수 없는 값([TimelineEmotion.UNKNOWN])은 null 로 내려
 * 표시 계층이 중립 상태를 고르게 한다.
 */
fun TimelineEmotion.toUiEmotionOrNull(): Emotion? =
    when (this) {
        TimelineEmotion.VERY_HAPPY -> Emotion.JOY
        TimelineEmotion.HAPPY -> Emotion.CALM
        TimelineEmotion.NEUTRAL -> Emotion.MELLOW
        TimelineEmotion.UNHAPPY -> Emotion.WEARY
        TimelineEmotion.VERY_UNHAPPY -> Emotion.DOWN
        TimelineEmotion.UNKNOWN -> null
    }
