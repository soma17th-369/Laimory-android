package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.coordinator.OnboardingCompletionCoordinator
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 온보딩 완료 여부를 관찰한다. `null` 이면 아직 모른다는 뜻이라 루트를 정하지 않는다. */
@Singleton
class ObserveOnboardingCompletionUseCase
    @Inject
    constructor(
        private val coordinator: OnboardingCompletionCoordinator,
    ) {
        operator fun invoke(): Flow<Boolean?> = coordinator.completed
    }
