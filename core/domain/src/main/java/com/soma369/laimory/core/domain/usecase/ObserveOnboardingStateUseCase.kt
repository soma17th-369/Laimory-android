package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.onboarding.OnboardingState
import com.soma369.laimory.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 설치 단위 온보딩 진행 상태를 관찰한다(앱 루트 결정·온보딩 화면 공유). */
@Singleton
class ObserveOnboardingStateUseCase
    @Inject
    constructor(
        private val repository: OnboardingRepository,
    ) {
        operator fun invoke(): Flow<OnboardingState> = repository.observe()
    }
