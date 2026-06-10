package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.intro.IntroResponse
import retrofit2.http.GET

interface IntroApi {
    @GET("v1/intro")
    suspend fun getIntroInfo(): IntroResponse
}
