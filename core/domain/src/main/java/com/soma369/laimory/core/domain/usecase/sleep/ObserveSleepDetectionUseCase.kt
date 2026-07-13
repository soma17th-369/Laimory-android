package com.soma369.laimory.core.domain.usecase.sleep

import com.soma369.laimory.core.domain.repository.SleepDetectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 수면 자동 감지 활성 여부를 관찰한다(#142). */
@Singleton
class ObserveSleepDetectionUseCase
    @Inject
    constructor(
        private val repository: SleepDetectionRepository,
    ) {
        operator fun invoke(): Flow<Boolean> = repository.observeEnabled()
    }
