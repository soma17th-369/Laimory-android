package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.credit.CreditCostsResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * 작업별 크레딧 비용. **인증이 필요 없다** — 사용자와 무관한 서버 상수라 잔액([CreditApi])과 경로·클라이언트를 나눈다.
 */
interface CreditCostApi {
    @GET("credit/costs")
    suspend fun getCosts(): Response<ApiResponse<CreditCostsResponse>>
}
