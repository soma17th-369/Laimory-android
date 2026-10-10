package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.onboarding.AppInitializerResponse

/**
 * 앱 초기화 조회(`GET /initializer`). 온보딩 완료 여부는 [OnboardingRemoteDataSource] 가 같은 응답에서 따로 읽는다 —
 * 그쪽은 캐시가 없을 때만 부르고, 팝업 공지는 콜드 스타트마다 최신 값이 필요해 호출 시점이 다르다.
 */
interface AppInitializerRemoteDataSource {
    suspend fun fetch(): AppInitializerResponse
}
