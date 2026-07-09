package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.SleepDetectionRepository
import kotlinx.coroutines.flow.Flow

/** 수면 자동 감지 활성 여부를 관찰한다(#142). */
class ObserveSleepDetectionUseCase(
    private val repository: SleepDetectionRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeEnabled()
}
