package com.soma369.laimory.core.domain.usecase.push

import com.soma369.laimory.core.domain.repository.PushRegistrationRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/** Firebase Messaging이 등록한 FID를 현재 인증 사용자에게 멱등 등록한다. */
@Singleton
class RegisterPushInstallationUseCase
    @Inject
    constructor(
        private val repository: PushRegistrationRepository,
    ) {
        suspend operator fun invoke(firebaseInstallationId: String): Result<Unit> =
            try {
                repository.register(firebaseInstallationId)
                Result.success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Result.failure(error)
            }
    }
