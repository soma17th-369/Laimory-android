package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.user.UserProfileResponse
import retrofit2.Response
import retrofit2.http.GET

/** 인증 사용자의 회원 정보 엔드포인트다. */
interface UserApi {
    /**
     * 현재 인증 세션 계정의 회원 정보를 조회한다.
     *
     * 응답에 닉네임이 담기므로 debug 에서도 BODY 를 로깅하지 않는 Retrofit 으로 만든다.
     */
    @GET("users/me")
    suspend fun getMyProfile(): Response<ApiResponse<UserProfileResponse>>
}
