package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.credit.CreditCostsResponse
import com.soma369.laimory.core.data.model.credit.CreditResponse

interface CreditRemoteDataSource {
    suspend fun getCredit(): CreditResponse

    suspend fun getCosts(): CreditCostsResponse
}
