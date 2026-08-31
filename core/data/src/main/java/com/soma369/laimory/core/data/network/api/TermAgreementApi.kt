package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.terms.TermAgreementHistoryResponse
import com.soma369.laimory.core.data.model.terms.request.TermAgreementCreateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** 인증 계정의 약관 동의 등록·이력 조회. 이 두 경로는 미동의 상태에서도 열려 있다. */
interface TermAgreementApi {
    /**
     * 동의를 일괄 등록한다. 성공 응답의 `body` 는 비어 있다.
     *
     * all-or-nothing 이라 하나라도 현재 유효 버전이 아니면 아무것도 기록되지 않고 409 다.
     */
    @POST("terms/agreements")
    suspend fun agreeToTerms(
        @Body request: TermAgreementCreateRequest,
    ): Response<ApiResponse<Unit>>

    /** 남아 있는 동의 이력 전부. 없으면 빈 배열이다. */
    @GET("terms/agreements")
    suspend fun getMyAgreements(): Response<ApiResponse<TermAgreementHistoryResponse>>
}
