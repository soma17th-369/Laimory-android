package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.UserProfileRemoteDataSource
import com.soma369.laimory.core.data.model.user.toDomain
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.repository.UserProfileRepository
import javax.inject.Inject

class UserProfileRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: UserProfileRemoteDataSource,
    ) : UserProfileRepository {
        override suspend fun getMyProfile(): UserProfile = remoteDataSource.getMyProfile().toDomain()
    }
