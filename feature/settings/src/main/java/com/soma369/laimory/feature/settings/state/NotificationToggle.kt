package com.soma369.laimory.feature.settings.state

/** 알림 화면이 다루는 항목. 어느 줄이 서버 응답을 기다리는지 가리는 데 쓴다. */
enum class NotificationToggle {
    /** 전체 푸시 수신. 끄면 모든 알림이 막힌다. */
    PUSH,

    /** 하루를 기록할 시간에 오는 리마인더. */
    DAILY_REMINDER,
}
