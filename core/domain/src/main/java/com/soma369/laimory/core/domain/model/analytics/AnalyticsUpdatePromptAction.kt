package com.soma369.laimory.core.domain.model.analytics

/** 업데이트 안내에서 고른 행동. 강제 안내에는 [UPDATE] 만 있다. */
enum class AnalyticsUpdatePromptAction {
    /** 스토어로 간다. */
    UPDATE,

    /** 권장 안내의 `나중에`. */
    LATER,

    /** 권장 안내를 바깥 터치 · 뒤로 가기로 닫았다. */
    DISMISS,
}
