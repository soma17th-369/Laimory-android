package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.domain.model.Feature1Item

interface Feature1RemoteDataSource {
    suspend fun getItems(): List<Feature1Item>

    suspend fun triggerServerError()

    suspend fun triggerUnauthorizedError()

    suspend fun triggerNetworkError()
}
