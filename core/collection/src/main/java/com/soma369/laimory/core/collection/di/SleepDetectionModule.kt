package com.soma369.laimory.core.collection.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.soma369.laimory.core.collection.health.DataStoreSleepClassifyStore
import com.soma369.laimory.core.collection.health.SleepClassifyDataStore
import com.soma369.laimory.core.collection.health.SleepClassifyStore
import com.soma369.laimory.core.collection.health.SleepDetectionDataStore
import com.soma369.laimory.core.collection.health.SleepDetectionRepositoryImpl
import com.soma369.laimory.core.collection.health.SleepHealthRecorder
import com.soma369.laimory.core.collection.health.SleepSegmentProcessor
import com.soma369.laimory.core.domain.repository.SleepDetectionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import javax.inject.Singleton

/**
 * Sleep API 감지 파이프라인 배선.
 *
 * 신뢰도 표본 버퍼 DataStore + store 바인딩 + segment 처리기 제공.
 * 처리기는 밤 키/오프셋 계산에 기기 시간대가 필요해 `ZoneId.systemDefault()` 를 지연 주입한다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SleepDetectionModule {
    @Binds
    abstract fun bindSleepClassifyStore(impl: DataStoreSleepClassifyStore): SleepClassifyStore

    @Binds
    abstract fun bindSleepDetectionRepository(impl: SleepDetectionRepositoryImpl): SleepDetectionRepository

    companion object {
        @Provides
        @Singleton
        @SleepClassifyDataStore
        fun provideSleepClassifyDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("sleep_classify") }

        @Provides
        @Singleton
        @SleepDetectionDataStore
        fun provideSleepDetectionDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("sleep_detection") }

        @Provides
        @Singleton
        fun provideSleepSegmentProcessor(
            recorder: SleepHealthRecorder,
            classifyStore: SleepClassifyStore,
        ): SleepSegmentProcessor = SleepSegmentProcessor(recorder, classifyStore) { ZoneId.systemDefault() }
    }
}
