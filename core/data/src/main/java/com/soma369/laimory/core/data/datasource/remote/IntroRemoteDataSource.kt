package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.intro.IntroResponse

interface IntroRemoteDataSource {
    suspend fun getIntroInfo(): IntroResponse
}
