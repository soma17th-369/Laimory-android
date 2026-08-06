package com.soma369.laimory.core.collection.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.soma369.laimory.core.collection.location.AndroidLocationAddressResolver
import com.soma369.laimory.core.collection.location.LocationSegmentStore
import com.soma369.laimory.core.collection.location.LocationTrackingDataStore
import com.soma369.laimory.core.collection.location.LocationTrackingRepositoryImpl
import com.soma369.laimory.core.collection.location.RoomLocationAddressRepository
import com.soma369.laimory.core.collection.location.RoomLocationSegmentStore
import com.soma369.laimory.core.domain.provider.LocationAddressResolver
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import com.soma369.laimory.core.domain.repository.MovementAddressRepository
import com.soma369.laimory.core.domain.repository.StayAddressRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 위치 추적 배선. 토글 의도 영속 DataStore + 리포지토리 바인딩(Phase 2: FGS 백그라운드 지속). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationModule {
    @Binds
    abstract fun bindLocationTrackingRepository(impl: LocationTrackingRepositoryImpl): LocationTrackingRepository

    @Binds
    abstract fun bindLocationSegmentStore(impl: RoomLocationSegmentStore): LocationSegmentStore

    @Binds
    abstract fun bindLocationAddressResolver(impl: AndroidLocationAddressResolver): LocationAddressResolver

    @Binds
    abstract fun bindStayAddressRepository(impl: RoomLocationAddressRepository): StayAddressRepository

    @Binds
    abstract fun bindMovementAddressRepository(impl: RoomLocationAddressRepository): MovementAddressRepository

    companion object {
        @Provides
        @Singleton
        @LocationTrackingDataStore
        fun provideLocationTrackingDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("location_tracking") }
    }
}
