package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.push.PushSettings

/** 계정 단위 푸시 수신 설정의 조회·변경. 서버가 단일 권위라 로컬 캐시를 두지 않는다. */
interface PushSettingsRepository {
    suspend fun getPushSettings(): PushSettings

    suspend fun updatePushEnabled(isEnabled: Boolean)

    suspend fun updateDailyReminderEnabled(isEnabled: Boolean)
}
