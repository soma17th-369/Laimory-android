package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.onboarding.AppInitializerResponse
import retrofit2.Response
import retrofit2.http.GET
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
     */
    @POST("onboarding/complete")
    suspend fun complete(): Response<ApiResponse<Unit>>

    /**
     * 인증 subject 의 저장된 온보딩 완료 여부를 조회한다.
     *
     * 경로에 `onboarding` 이 없다 — 서버는 앱 시작에 필요한 설정을 한곳에 모아 주는 자리로 두고
     * 있다. 설정 행이 없는 사용자에게는 기본값 대신 500 을 낸다(운영 신호)이므로, 호출부는
     * 실패를 "완료 아님" 으로 떨어뜨리고 앱 진입을 막지 않아야 한다.
     */
    @GET("initializer")
    suspend fun getInitializer(): Response<ApiResponse<AppInitializerResponse>>
}
