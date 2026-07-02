package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.feature1.Feature1ItemResponse
import com.soma369.laimory.core.data.network.BaseRemoteDataSource
import com.soma369.laimory.core.data.network.api.Feature1Api
import kotlinx.serialization.json.Json
import javax.inject.Inject

class Feature1RemoteDataSourceImpl
    @Inject
    constructor(
        private val api: Feature1Api,
        json: Json,
    ) : BaseRemoteDataSource(json),
        Feature1RemoteDataSource {
        override suspend fun getItems(): List<Feature1ItemResponse> = safeApiCall { api.getItems() }

        override suspend fun triggerServerError() = safeApiCallForCompletion { api.triggerServerError() }

        override suspend fun triggerUnauthorizedError() = safeApiCallForCompletion { api.triggerUnauthorizedError() }

        override suspend fun triggerNetworkError() = safeApiCallForCompletion { api.triggerNetworkError() }
    }
