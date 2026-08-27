package com.soma369.laimory.core.data.datasource.remote

/** 온보딩 이력 API 호출을 캡슐화하는 원격 데이터 소스다. */
interface OnboardingRemoteDataSource {
    /** 인증 subject 의 온보딩 완료를 기록한다. 멱등이라 재시도가 안전하다. */
    suspend fun recordCompletion()
}
