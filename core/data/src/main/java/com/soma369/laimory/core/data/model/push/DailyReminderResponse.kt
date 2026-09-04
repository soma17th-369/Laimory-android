package com.soma369.laimory.core.data.model.push

import kotlinx.serialization.Serializable

/**
 * 일일 리마인더 설정.
 *
 * 서버는 `time`(`HH:mm`)과 광고성 분류도 함께 주지만, 시각 변경이 이번 화면 범위 밖이라 받지 않는다.
 * 필요해지면 여기에 필드를 더하고 도메인까지 잇는다.
 */
@Serializable
data class DailyReminderResponse(
    val enabled: Boolean,
)
