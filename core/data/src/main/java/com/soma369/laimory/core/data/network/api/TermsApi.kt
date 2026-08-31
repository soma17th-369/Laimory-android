package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.terms.TermListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 약관 공개 조회. **인증이 필요 없다** — 로그인 전 화면도 같은 경로로 원문 주소를 얻는다.
 */
interface TermsApi {
    /**
     * 요청한 종류의 현재 유효 약관을 **요청한 순서대로** 받는다.
     *
     * 같은 query key 를 반복해 보내는 계약이라 목록을 그대로 넘긴다. 중복은 400 이므로 호출부가
     * 중복 없는 목록을 준다. 아직 활성화되지 않은 종류는 응답에서 빠지고, 전부 없으면 빈 배열이다.
     */
    @GET("terms")
    suspend fun getCurrentTerms(
        @Query("termTypes") termTypes: List<String>,
    ): Response<ApiResponse<TermListResponse>>
}
