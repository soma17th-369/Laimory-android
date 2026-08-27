package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.coordinator.OnboardingCompletionCoordinator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 온보딩을 완료로 확정한다.
 *
 * 완료 여부는 계정 단위라 서버가 정본이다. 조율자가 세션 값을 올리고 캐시와 서버 기록을 함께
 * 갱신하면, 앱 루트가 그것을 보고 스스로 Home 으로 바뀐다.
 */
@Singleton
class CompleteOnboardingUseCase
    @Inject
    constructor(
        private val coordinator: OnboardingCompletionCoordinator,
    ) {
        suspend operator fun invoke() = coordinator.markCompleted()
    }
