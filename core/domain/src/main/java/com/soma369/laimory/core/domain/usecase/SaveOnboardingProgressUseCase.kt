package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.OnboardingRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 마지막으로 본 온보딩 페이지를 기록해 중간에 나가도 이어 볼 수 있게 한다. */
@Singleton
class SaveOnboardingProgressUseCase
    @Inject
    constructor(
        private val repository: OnboardingRepository,
    ) {
        suspend operator fun invoke(pageKey: String) = repository.saveProgress(pageKey)
    }
