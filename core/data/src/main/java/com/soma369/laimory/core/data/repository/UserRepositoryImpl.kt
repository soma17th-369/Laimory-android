package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.UserRemoteDataSource
import com.soma369.laimory.core.data.model.user.toDomain
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.repository.UserRepository
import javax.inject.Inject

class UserRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: UserRemoteDataSource,
    ) : UserRepository {
        override suspend fun getMyProfile(): UserProfile = remoteDataSource.getMyProfile().toDomain()

        override suspend fun requestAccountWithdrawal() = remoteDataSource.requestAccountWithdrawal()
    }
