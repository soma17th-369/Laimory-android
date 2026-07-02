package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.feature1.Feature1ItemResponse
import retrofit2.Response
import retrofit2.http.GET

interface Feature1Api {
    @GET("v1/feature1/items")
    suspend fun getItems(): Response<ApiResponse<List<Feature1ItemResponse>>>

    @GET("v1/feature1/error/server")
    suspend fun triggerServerError(): Response<ApiResponse<Unit>>

    @GET("v1/feature1/error/unauthorized")
    suspend fun triggerUnauthorizedError(): Response<ApiResponse<Unit>>

    @GET("v1/feature1/error/network")
    suspend fun triggerNetworkError(): Response<ApiResponse<Unit>>
}
