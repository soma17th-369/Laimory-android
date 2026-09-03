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
 * 앱 설정 저장소.
 *
 * 인증·온보딩 저장소와 파일을 나눈다. 저 둘은 계정이나 설치 흐름의 경계에서 비워지는데, 화면
 * 모드는 로그아웃해도 남아야 한다 — 이 기기를 쓰는 사람이 정한 값이다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AppSettingsStorageModule {
    const val STORE_FILE_NAME = "app_settings"

    @Provides
    @Singleton
    @AppSettingsDataStore
    fun provideAppSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(STORE_FILE_NAME) }
}
