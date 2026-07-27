package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.push.PushRegistrationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.PUT

/** 인증 사용자의 Firebase Installation ID 등록·해제 엔드포인트다. */
interface PushRegistrationApi {
    /** FID를 현재 인증 사용자의 FCM 수신 대상으로 등록한다. */
    @PUT("push-registrations")
    suspend fun register(
        @Body request: PushRegistrationRequest,
    ): Response<ApiResponse<Unit>>

    /** FID와 현재 인증 사용자의 FCM 수신 연결을 해제한다. */
    @HTTP(method = "DELETE", path = "push-registrations", hasBody = true)
    suspend fun unregister(
        @Body request: PushRegistrationRequest,
    ): Response<ApiResponse<Unit>>
}
