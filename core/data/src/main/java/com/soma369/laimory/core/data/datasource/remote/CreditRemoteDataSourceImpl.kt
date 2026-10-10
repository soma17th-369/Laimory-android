package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.credit.CreditCostsResponse
import com.soma369.laimory.core.data.model.credit.CreditResponse
import com.soma369.laimory.core.data.network.api.CreditApi
import com.soma369.laimory.core.data.network.api.CreditCostApi
import com.soma369.laimory.core.data.network.safeApiCall
import javax.inject.Inject

class CreditRemoteDataSourceImpl
    @Inject
    constructor(
        private val creditApi: CreditApi,
        private val creditCostApi: CreditCostApi,
    ) : CreditRemoteDataSource {
        override suspend fun getCredit(): CreditResponse = safeApiCall { creditApi.getCredit() }

        override suspend fun getCosts(): CreditCostsResponse = safeApiCall { creditCostApi.getCosts() }
    }
