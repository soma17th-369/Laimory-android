package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.Feature1RemoteDataSource
import com.soma369.laimory.core.data.model.feature1.toDomain
import com.soma369.laimory.core.domain.model.Feature1Item
import com.soma369.laimory.core.domain.repository.Feature1Repository
import javax.inject.Inject

class Feature1RepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: Feature1RemoteDataSource,
    ) : Feature1Repository {
        override suspend fun getItems(): List<Feature1Item> = remoteDataSource.getItems().map { it.toDomain() }

        override suspend fun triggerServerError() = remoteDataSource.triggerServerError()

        override suspend fun triggerUnauthorizedError() = remoteDataSource.triggerUnauthorizedError()

        override suspend fun triggerNetworkError() = remoteDataSource.triggerNetworkError()
    }
