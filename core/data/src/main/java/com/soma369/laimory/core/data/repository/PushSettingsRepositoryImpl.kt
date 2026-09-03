package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.PushSettingsRemoteDataSource
import com.soma369.laimory.core.domain.model.push.PushSettings
import com.soma369.laimory.core.domain.repository.PushSettingsRepository
import javax.inject.Inject

internal class PushSettingsRepositoryImpl
    @Inject
    constructor(
        private val remote: PushSettingsRemoteDataSource,
    ) : PushSettingsRepository {
        override suspend fun getPushSettings(): PushSettings =
            remote.getPushSettings().let { response ->
                PushSettings(
                    isPushEnabled = response.pushEnabled,
                    isDailyReminderEnabled = response.dailyReminder.enabled,
                )
            }

        override suspend fun updatePushEnabled(isEnabled: Boolean) {
            remote.updatePushEnabled(isEnabled)
        }

        override suspend fun updateDailyReminderEnabled(isEnabled: Boolean) {
            remote.updateDailyReminderEnabled(isEnabled)
        }
    }
