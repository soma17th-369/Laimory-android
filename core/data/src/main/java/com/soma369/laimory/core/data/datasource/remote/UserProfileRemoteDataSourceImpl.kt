package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.user.UserProfileResponse
import com.soma369.laimory.core.data.network.api.UserApi
import com.soma369.laimory.core.data.network.safeApiCall
import javax.inject.Inject

class UserProfileRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: UserApi,
    ) : UserProfileRemoteDataSource {
        override suspend fun getMyProfile(): UserProfileResponse = safeApiCall { api.getMyProfile() }
    }
