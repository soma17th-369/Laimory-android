package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.user.UserProfileResponse

/** 회원 정보 API 호출을 캡슐화하는 원격 데이터 소스다. */
interface UserProfileRemoteDataSource {
    /** 현재 인증 세션 계정의 회원 정보를 조회한다. */
    suspend fun getMyProfile(): UserProfileResponse
}
