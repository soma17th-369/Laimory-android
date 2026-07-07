package com.soma369.laimory.core.collection.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.soma369.laimory.core.collection.location.LocationTrackingDataStore
import com.soma369.laimory.core.collection.location.LocationTrackingRepositoryImpl
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 위치 추적 토글 저장(DataStore) 배선. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationModule {
    @Binds
    abstract fun bindLocationTrackingRepository(impl: LocationTrackingRepositoryImpl): LocationTrackingRepository

    companion object {
        @Provides
        @Singleton
        @LocationTrackingDataStore
        fun provideLocationTrackingDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("location_tracking") }
    }
}
