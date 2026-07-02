package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.intro.IntroResponse
import com.soma369.laimory.core.data.network.api.IntroApi
import com.soma369.laimory.core.data.network.safeApiCall
import javax.inject.Inject

class IntroRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: IntroApi,
    ) : IntroRemoteDataSource {
        override suspend fun getIntroInfo(): IntroResponse = safeApiCall { api.getIntroInfo() }
    }
