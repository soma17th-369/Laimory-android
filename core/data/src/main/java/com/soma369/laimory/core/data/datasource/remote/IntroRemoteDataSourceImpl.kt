package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.intro.IntroResponse
import com.soma369.laimory.core.data.network.BaseRemoteDataSource
import com.soma369.laimory.core.data.network.api.IntroApi
import kotlinx.serialization.json.Json
import javax.inject.Inject

class IntroRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: IntroApi,
        json: Json,
    ) : BaseRemoteDataSource(json),
        IntroRemoteDataSource {
        override suspend fun getIntroInfo(): IntroResponse = safeApiCall { api.getIntroInfo() }
    }
