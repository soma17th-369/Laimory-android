package com.soma369.laimory.core.collection.di

import com.soma369.laimory.core.collection.location.LocationTrackingRepositoryImpl
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 위치 추적 배선. 토글은 세션 한정이라 매니저 상태에 위임하며 영속 저장하지 않는다(Phase 1). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationModule {
    @Binds
    abstract fun bindLocationTrackingRepository(impl: LocationTrackingRepositoryImpl): LocationTrackingRepository
}
