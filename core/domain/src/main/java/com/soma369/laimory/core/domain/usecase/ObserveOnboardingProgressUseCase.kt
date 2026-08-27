package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 마지막으로 본 온보딩 장의 키를 관찰한다(중간에 나갔다 오면 거기서 이어 본다). */
@Singleton
class ObserveOnboardingProgressUseCase
    @Inject
    constructor(
        private val repository: OnboardingRepository,
    ) {
        operator fun invoke(): Flow<String?> = repository.observeLastPageKey()
    }
