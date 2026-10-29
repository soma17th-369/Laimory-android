package com.soma369.laimory.core.domain.model.reminder

import java.time.LocalTime

/**
 * 일일 리마인더 설정. 기기에 두는 값이라 로그아웃해도 남는다.
 *
 * 예전에는 서버가 계정 단위로 21:00 에 보냈다. 이제 앱이 기기 시간대 기준으로 직접 예약하고, 서버
 * 발송은 앱이 조용히 끈다(`HandOffDailyReminderUseCase`).
 */
data class DailyReminderSettings(
    val isEnabled: Boolean,
    val time: LocalTime,
) {
    companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(21, 0)

        val DEFAULT = DailyReminderSettings(isEnabled = true, time = DEFAULT_TIME)
    }
}
