package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.OnboardingRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 온보딩 상태를 처음으로 되돌린다.
 *
 * QA 가 흐름을 반복해서 보기 위한 것이라 release 진입점을 두지 않는다. 화면에 체크박스를 두는
 * 대신 이 경로를 둔 이유는, 체크박스가 있으면 사용자가 "체크 안 하면 매번 뜨나" 로 읽기 때문이다.
 */
@Singleton
class ResetOnboardingUseCase
    @Inject
    constructor(
        private val repository: OnboardingRepository,
    ) {
        suspend operator fun invoke() = repository.reset()
    }
