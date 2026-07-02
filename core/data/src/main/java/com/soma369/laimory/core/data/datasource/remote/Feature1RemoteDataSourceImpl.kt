package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.feature1.Feature1ItemResponse
import com.soma369.laimory.core.data.network.api.Feature1Api
import com.soma369.laimory.core.data.network.safeApiCall
import javax.inject.Inject

class Feature1RemoteDataSourceImpl
    @Inject
    constructor(
        private val api: Feature1Api,
    ) : Feature1RemoteDataSource {
        override suspend fun getItems(): List<Feature1ItemResponse> = safeApiCall { api.getItems() }

        override suspend fun triggerServerError() {
            safeApiCall { api.triggerServerError() }
        }

        override suspend fun triggerUnauthorizedError() {
            safeApiCall { api.triggerUnauthorizedError() }
        }

        override suspend fun triggerNetworkError() {
            safeApiCall { api.triggerNetworkError() }
        }
    }
