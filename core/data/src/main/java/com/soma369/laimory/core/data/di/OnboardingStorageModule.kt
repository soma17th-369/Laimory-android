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
 * 인증 저장소([AuthStorageModule])와 **파일을 나눈다.** 온보딩 완료는 로그아웃·탈퇴로 지워지면
 * 안 되는데, 같은 파일에 얹으면 세션 정리가 통째로 비우면서 함께 사라진다.
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
