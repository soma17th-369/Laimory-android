package com.soma369.laimory.core.domain.model.analytics

/** 알림을 눌렀을 때 앱이 어떻게 열렸는지. */
enum class AnalyticsLaunchState {
    /** 화면을 이전 상태 없이 새로 만들었다. */
    COLD,

    /** 떠 있던 화면이 알림을 받았거나, 저장된 상태로 화면을 되살렸다. */
    WARM,
}
