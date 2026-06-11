package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.feature1.Feature1ItemResponse

interface Feature1RemoteDataSource {
    suspend fun getItems(): List<Feature1ItemResponse>

    suspend fun triggerServerError()

    suspend fun triggerUnauthorizedError()

    suspend fun triggerNetworkError()
}
