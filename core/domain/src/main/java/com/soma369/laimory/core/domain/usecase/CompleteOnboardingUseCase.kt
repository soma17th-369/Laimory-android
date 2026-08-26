package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.OnboardingRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 온보딩을 완료로 확정한다.
 *
 * 저장이 끝나야 Home 으로 넘어간다 — 저장 전에 넘기면 그 사이 앱이 죽었을 때 온보딩을 끝낸
 * 사용자가 다음 실행에서 처음부터 다시 본다.
 */
@Singleton
class CompleteOnboardingUseCase
    @Inject
    constructor(
        private val repository: OnboardingRepository,
    ) {
        suspend operator fun invoke() = repository.complete()
    }
