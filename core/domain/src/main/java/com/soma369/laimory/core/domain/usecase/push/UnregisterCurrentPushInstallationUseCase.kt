package com.soma369.laimory.core.domain.usecase.push

import com.soma369.laimory.core.domain.provider.PushInstallationIdProvider
import com.soma369.laimory.core.domain.repository.PushRegistrationRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/** 현재 FID의 서버 결합을 해제한다. Firebase 설치 자체는 삭제하지 않는다. */
@Singleton
class UnregisterCurrentPushInstallationUseCase
    @Inject
    constructor(
        private val installationIdProvider: PushInstallationIdProvider,
        private val repository: PushRegistrationRepository,
    ) {
        suspend operator fun invoke(): Result<Unit> =
            try {
                repository.unregister(installationIdProvider.getCurrentId())
                Result.success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Result.failure(error)
            }
    }
