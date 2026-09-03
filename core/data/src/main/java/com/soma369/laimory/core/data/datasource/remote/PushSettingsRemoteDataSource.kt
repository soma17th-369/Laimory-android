package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.push.PushSettingsResponse

interface PushSettingsRemoteDataSource {
    suspend fun getPushSettings(): PushSettingsResponse

    suspend fun updatePushEnabled(isEnabled: Boolean)

    suspend fun updateDailyReminderEnabled(isEnabled: Boolean)
}
