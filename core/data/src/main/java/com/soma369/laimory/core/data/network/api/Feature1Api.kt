package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.feature1.Feature1ItemResponse
import retrofit2.http.GET

interface Feature1Api {
    @GET("v1/feature1/items")
    suspend fun getItems(): Result<List<Feature1ItemResponse>>
}
