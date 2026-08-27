package com.soma369.laimory.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 온보딩 진행 상태 저장소.
 *
 * 인증 저장소([AuthStorageModule])와 **파일을 나눈다.** 세션 정리가 통째로 비우는 자리에 얹으면
 * 무엇을 언제 비울지 이 모듈이 정할 수 없다. 완료 여부는 계정 단위 서버 값의 캐시라 계정 경계에서
 * 비우고, 진행 위치는 그 사이에도 남겨야 한다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object OnboardingStorageModule {
    const val STORE_FILE_NAME = "onboarding_progress"

    @Provides
    @Singleton
    @OnboardingDataStore
    fun provideOnboardingDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(STORE_FILE_NAME) }
}
