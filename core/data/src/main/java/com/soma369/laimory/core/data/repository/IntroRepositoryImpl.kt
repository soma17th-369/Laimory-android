package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.IntroRemoteDataSource
import com.soma369.laimory.core.data.model.intro.toDomain
import com.soma369.laimory.core.domain.model.IntroInfo
import com.soma369.laimory.core.domain.repository.IntroRepository
import javax.inject.Inject

class IntroRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: IntroRemoteDataSource,
    ) : IntroRepository {
        override suspend fun getIntroInfo(): IntroInfo = remoteDataSource.getIntroInfo().toDomain()
    }
