package com.soma369.laimory.core.domain.model.push

/**
 * 계정 단위 푸시 수신 설정. 서버가 권위이며 기기에 복제 저장하지 않는다.
 *
 * 기기의 알림 표시 권한(`POST_NOTIFICATIONS`)과는 다른 값이다 — 이쪽은 "서버가 이 계정에 보낼지",
 * 저쪽은 "이 기기가 받은 것을 띄울지"다. 둘 중 하나만 꺼져도 알림은 보이지 않는다.
 *
 * @param isPushEnabled 전체 푸시 수신. 꺼도 종류별 설정값은 서버에 그대로 남는다.
 * @param isDailyReminderEnabled 하루를 기록할 시간에 오는 리마인더.
 */
data class PushSettings(
    val isPushEnabled: Boolean,
    val isDailyReminderEnabled: Boolean,
)
