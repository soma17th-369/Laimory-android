package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.push.PushEnabledRequest
import com.soma369.laimory.core.data.model.push.PushSettingsResponse
import com.soma369.laimory.core.data.network.api.PushSettingsApi
import com.soma369.laimory.core.data.network.safeApiCall
import com.soma369.laimory.core.data.network.safeApiCallUnit
import javax.inject.Inject

class PushSettingsRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: PushSettingsApi,
    ) : PushSettingsRemoteDataSource {
        override suspend fun getPushSettings(): PushSettingsResponse = safeApiCall { api.getPushSettings() }

        override suspend fun updatePushEnabled(isEnabled: Boolean) {
            safeApiCallUnit { api.updatePushEnabled(PushEnabledRequest(isEnabled)) }
        }

        override suspend fun updateDailyReminderEnabled(isEnabled: Boolean) {
            safeApiCallUnit { api.updateDailyReminderEnabled(PushEnabledRequest(isEnabled)) }
        }
    }
