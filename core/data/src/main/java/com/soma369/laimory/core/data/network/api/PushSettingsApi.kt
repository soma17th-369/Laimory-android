package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.push.PushEnabledRequest
import com.soma369.laimory.core.data.model.push.PushSettingsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/** 인증 사용자의 푸시 수신 설정 엔드포인트다. 계정 단위이며 기기와 무관하다. */
interface PushSettingsApi {
    @GET("push-settings")
    suspend fun getPushSettings(): Response<ApiResponse<PushSettingsResponse>>

    @PUT("push-settings/enabled")
    suspend fun updatePushEnabled(
        @Body request: PushEnabledRequest,
    ): Response<ApiResponse<Unit>>

    @PUT("push-settings/daily-reminder/enabled")
    suspend fun updateDailyReminderEnabled(
        @Body request: PushEnabledRequest,
    ): Response<ApiResponse<Unit>>
}
