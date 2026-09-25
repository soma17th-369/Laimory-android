package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.OnboardingRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 이번 온보딩 회차의 분석용 토큰. 처음 부르면 만들어 남기고, 이어 보는 동안 같은 값을 준다. */
@Singleton
class GetOnboardingFlowIdUseCase
    @Inject
    constructor(
        private val repository: OnboardingRepository,
    ) {
        suspend operator fun invoke(): String = repository.flowId()
    }
