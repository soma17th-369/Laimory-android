package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 위치 자동 수집(추적)을 켜거나 끈다. */
@Singleton
class SetLocationTrackingUseCase
    @Inject
    constructor(
        private val repository: LocationTrackingRepository,
    ) {
        suspend operator fun invoke(enabled: Boolean) = repository.setEnabled(enabled)
    }
