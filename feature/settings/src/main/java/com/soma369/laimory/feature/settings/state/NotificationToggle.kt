package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.domain.model.push.PushSettings

/** 알림 화면이 다루는 항목. 어느 줄을 켜고 끄는지 가리는 데 쓴다. */
enum class NotificationToggle {
    /** 전체 푸시 수신. 끄면 모든 알림이 막힌다. */
    PUSH,

    /** 하루를 기록할 시간에 오는 리마인더. */
    DAILY_REMINDER,
}

/** [toggle] 줄이 가리키는 값. */
fun PushSettings.isEnabled(toggle: NotificationToggle): Boolean =
    when (toggle) {
        NotificationToggle.PUSH -> isPushEnabled
        NotificationToggle.DAILY_REMINDER -> isDailyReminderEnabled
    }

/** [toggle] 줄만 [isEnabled] 로 바꾼 사본. */
fun PushSettings.with(
    toggle: NotificationToggle,
    isEnabled: Boolean,
): PushSettings =
    when (toggle) {
        NotificationToggle.PUSH -> copy(isPushEnabled = isEnabled)
        NotificationToggle.DAILY_REMINDER -> copy(isDailyReminderEnabled = isEnabled)
    }
