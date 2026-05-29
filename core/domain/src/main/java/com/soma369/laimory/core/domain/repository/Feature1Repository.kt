package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.Feature1Item

interface Feature1Repository {
    suspend fun getItems(): List<Feature1Item>

    suspend fun triggerServerError()

    suspend fun triggerUnauthorizedError()

    suspend fun triggerNetworkError()
}
