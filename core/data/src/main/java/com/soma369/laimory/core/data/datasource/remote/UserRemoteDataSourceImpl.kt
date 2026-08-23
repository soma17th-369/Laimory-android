package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.user.UserProfileResponse
import com.soma369.laimory.core.data.network.api.UserApi
import com.soma369.laimory.core.data.network.safeApiCall
import com.soma369.laimory.core.data.network.safeApiCallUnit
import javax.inject.Inject

class UserRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: UserApi,
    ) : UserRemoteDataSource {
        override suspend fun getMyProfile(): UserProfileResponse = safeApiCall { api.getMyProfile() }

        // 탈퇴 성공(202)은 공통 envelope 의 body 가 null 이라 safeApiCallUnit 을 쓴다.
        override suspend fun requestAccountWithdrawal() = safeApiCallUnit { api.withdraw() }
    }
