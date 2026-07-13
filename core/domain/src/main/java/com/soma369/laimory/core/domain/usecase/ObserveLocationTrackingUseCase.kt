package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 위치 자동 수집(추적) 활성 여부를 관찰한다. */
@Singleton
class ObserveLocationTrackingUseCase
    @Inject
    constructor(
        private val repository: LocationTrackingRepository,
    ) {
        operator fun invoke(): Flow<Boolean> = repository.observeEnabled()
    }
