package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.feature1.toDomain
import com.soma369.laimory.core.data.network.api.Feature1Api
import com.soma369.laimory.core.domain.model.Feature1Item
import javax.inject.Inject

class Feature1RemoteDataSourceImpl
    @Inject
    constructor(
        private val api: Feature1Api,
    ) : Feature1RemoteDataSource {
        override suspend fun getItems(): List<Feature1Item> = api.getItems().getOrThrow().map { it.toDomain() }
    }
