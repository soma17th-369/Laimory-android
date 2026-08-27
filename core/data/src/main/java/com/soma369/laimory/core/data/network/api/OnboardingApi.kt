package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import retrofit2.Response
import retrofit2.http.POST

/** 계정 단위 온보딩 이력 엔드포인트다. */
interface OnboardingApi {
    /**
     * 인증 subject 의 온보딩 완료를 기록한다. request body 는 없다 — 대상은 언제나 인증 subject 이고
     * 바꿀 값도 하나뿐이다.
     *
     * 이미 완료한 subject 의 반복 호출도 같은 200 으로 멱등 성공하므로 재시도가 안전하다.
     * `false` 로 되돌리는 API 는 서버가 제공하지 않는다.
     *
     * 조회는 `GET /initializer` 의 `onboardingCompleted` 로 제공되지만 앱은 쓰지 않는다 —
     * 자동 노출 판정은 설치 단위 로컬 상태가 정본이다.
     */
    @POST("onboarding/complete")
    suspend fun complete(): Response<ApiResponse<Unit>>
}
