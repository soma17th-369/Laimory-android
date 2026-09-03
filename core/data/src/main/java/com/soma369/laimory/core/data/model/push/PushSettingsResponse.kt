package com.soma369.laimory.core.data.model.push

import kotlinx.serialization.Serializable

/**
 * `GET /push-settings` 응답.
 *
 * 서버는 광고성·야간 광고성 동의와 최근 처리결과도 함께 내려주지만 이 화면이 다루지 않아 받지 않는다.
 * 알 수 없는 키는 파서가 무시한다.
 */
@Serializable
data class PushSettingsResponse(
    val pushEnabled: Boolean,
    val dailyReminder: DailyReminderResponse,
)
