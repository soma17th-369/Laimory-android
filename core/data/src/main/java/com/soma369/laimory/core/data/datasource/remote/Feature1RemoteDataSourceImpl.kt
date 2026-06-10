package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.feature1.Feature1ItemResponse
import com.soma369.laimory.core.data.network.api.Feature1Api
import javax.inject.Inject

class Feature1RemoteDataSourceImpl
    @Inject
    constructor(
        private val api: Feature1Api,
    ) : Feature1RemoteDataSource {
        override suspend fun getItems(): List<Feature1ItemResponse> = api.getItems().getOrThrow()

        override suspend fun triggerServerError() {
            api.triggerServerError().getOrThrow()
        }

        override suspend fun triggerUnauthorizedError() {
            api.triggerUnauthorizedError().getOrThrow()
        }

        override suspend fun triggerNetworkError() {
            api.triggerNetworkError().getOrThrow()
        }
    }
