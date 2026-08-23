package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.user.UserProfileResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET

/** 인증 사용자의 회원 계정 엔드포인트다. */
interface UserApi {
    /**
     * 현재 인증 세션 계정의 회원 정보를 조회한다.
     *
     * 응답에 닉네임이 담기므로 debug 에서도 BODY 를 로깅하지 않는 Retrofit 으로 만든다.
     */
    @GET("users/me")
    suspend fun getMyProfile(): Response<ApiResponse<UserProfileResponse>>

    /**
     * 현재 인증 세션 계정의 탈퇴를 요청한다. request body 는 없다.
     *
     * 성공은 `202 Accepted` 이며 공통 envelope 의 `body` 가 `null` 이다(HTTP 무바디가 아니다).
     * 논리 탈퇴·credential 차단·push 등록 삭제와 데이터 삭제 작업 접수가 commit 됐다는 뜻이고
     * 물리 삭제 완료를 뜻하지 않는다. 접수 뒤 같은 access token 의 새 요청은 `401` 로 끝난다.
     */
    @DELETE("users/me")
    suspend fun withdraw(): Response<ApiResponse<Unit>>
}
