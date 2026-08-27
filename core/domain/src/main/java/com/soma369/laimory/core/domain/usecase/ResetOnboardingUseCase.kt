package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.coordinator.OnboardingCompletionCoordinator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 온보딩을 이 세션에 한해 처음으로 되돌린다.
 *
 * QA 가 흐름을 반복해서 보기 위한 것이라 release 진입점을 두지 않는다. 서버에는 `false` 로
 * 되돌리는 API 가 없으므로 **다시 로그인하면 서버 값(완료)이 그대로 돌아온다** — 세션 안에서만
 * 유효한 도구다.
 */
@Singleton
class ResetOnboardingUseCase
    @Inject
    constructor(
        private val coordinator: OnboardingCompletionCoordinator,
    ) {
        suspend operator fun invoke() = coordinator.resetForCurrentSession()
    }
