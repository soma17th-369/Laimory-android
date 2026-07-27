package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.push.PushRegistrationRequest
import com.soma369.laimory.core.data.network.api.PushRegistrationApi
import com.soma369.laimory.core.data.network.safeApiCallUnit
import javax.inject.Inject

class PushRegistrationRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: PushRegistrationApi,
    ) : PushRegistrationRemoteDataSource {
        override suspend fun register(firebaseInstallationId: String) {
            safeApiCallUnit { api.register(PushRegistrationRequest(firebaseInstallationId)) }
        }

        override suspend fun unregister(firebaseInstallationId: String) {
            safeApiCallUnit { api.unregister(PushRegistrationRequest(firebaseInstallationId)) }
        }
    }
