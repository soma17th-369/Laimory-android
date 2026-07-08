package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.intro.IntroResponse
import retrofit2.Response
import retrofit2.http.GET

interface IntroApi {
    @GET("intro")
    suspend fun getIntroInfo(): Response<ApiResponse<IntroResponse>>
}
