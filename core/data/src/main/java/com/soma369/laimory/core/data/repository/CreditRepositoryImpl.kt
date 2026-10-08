package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.CreditRemoteDataSource
import com.soma369.laimory.core.data.model.credit.toDomain
import com.soma369.laimory.core.domain.model.credit.CreditCosts
import com.soma369.laimory.core.domain.repository.CreditRepository
import javax.inject.Inject

/** 잔액은 다른 기기·결과 저장으로 바뀌는 서버 값이라 기기에 두지 않는다. 부를 때마다 서버에 묻는다. */
internal class CreditRepositoryImpl
    @Inject
    constructor(
        private val remoteDataSource: CreditRemoteDataSource,
    ) : CreditRepository {
        override suspend fun getRemainingCredits(): Int = remoteDataSource.getCredit().remainingCredits

        override suspend fun getCosts(): CreditCosts = remoteDataSource.getCosts().toDomain()
    }
