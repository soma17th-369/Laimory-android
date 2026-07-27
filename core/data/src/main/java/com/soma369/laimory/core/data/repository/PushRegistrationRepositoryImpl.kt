package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.PushRegistrationRemoteDataSource
import com.soma369.laimory.core.domain.repository.PushRegistrationRepository
import javax.inject.Inject

internal class PushRegistrationRepositoryImpl
    @Inject
    constructor(
        private val remote: PushRegistrationRemoteDataSource,
    ) : PushRegistrationRepository {
        override suspend fun register(firebaseInstallationId: String) {
            remote.register(firebaseInstallationId)
        }

        override suspend fun unregister(firebaseInstallationId: String) {
            remote.unregister(firebaseInstallationId)
        }
    }
