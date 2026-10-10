package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.credit.CreditResponse
import retrofit2.Response
import retrofit2.http.GET

/** 로그인 계정의 크레딧. 크레딧 행이 없으면 서버는 기본값 대신 500 을 낸다. */
interface CreditApi {
    @GET("credit")
    suspend fun getCredit(): Response<ApiResponse<CreditResponse>>
}
