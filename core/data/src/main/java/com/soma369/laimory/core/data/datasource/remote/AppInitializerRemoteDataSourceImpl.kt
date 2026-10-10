package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.onboarding.AppInitializerResponse
import com.soma369.laimory.core.data.network.api.OnboardingApi
import com.soma369.laimory.core.data.network.safeApiCall
import javax.inject.Inject

class AppInitializerRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: OnboardingApi,
    ) : AppInitializerRemoteDataSource {
        override suspend fun fetch(): AppInitializerResponse = safeApiCall { api.getInitializer() }
    }
