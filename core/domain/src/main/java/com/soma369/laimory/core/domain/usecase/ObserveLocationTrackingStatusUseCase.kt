package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 위치 자동 수집의 라이브 상태(체류 중/이동 중)를 관찰한다(추적 중이 아니면 null). */
@Singleton
class ObserveLocationTrackingStatusUseCase
    @Inject
    constructor(
        private val repository: LocationTrackingRepository,
    ) {
        operator fun invoke(): Flow<LocationTrackingStatus?> = repository.observeStatus()
    }
