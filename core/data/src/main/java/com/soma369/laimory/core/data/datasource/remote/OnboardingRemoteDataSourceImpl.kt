package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.network.api.OnboardingApi
import com.soma369.laimory.core.data.network.safeApiCall
import com.soma369.laimory.core.data.network.safeApiCallUnit
import javax.inject.Inject

class OnboardingRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: OnboardingApi,
    ) : OnboardingRemoteDataSource {
        override suspend fun recordCompletion() {
            safeApiCallUnit { api.complete() }
        }

        override suspend fun fetchCompletion(): Boolean = safeApiCall { api.getInitializer() }.onboardingCompleted
    }
