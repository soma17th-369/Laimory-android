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
 * 제품 분석 저장소.
 *
 * 다른 저장소와 **파일을 나눈다.** 인증 저장소에 얹으면 로그아웃이 통째로 비울 때 "이미 보낸 이벤트"
 * 기록과 수집 동의까지 사라진다 — 같은 사건이 다시 나가고, 다시 로그인할 때마다 동의를 새로 받게 된다.
 *
 * 판정 키와 동의도 서로 파일을 나눈다. 판정 키는 이벤트가 정하는 임의 문자열이라 한 파일에 두면
 * 동의 키와 이름이 겹칠 여지가 생긴다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsStorageModule {
    const val STORE_FILE_NAME = "analytics_dedupe"
    const val CONSENT_STORE_FILE_NAME = "analytics_consent"

    @Provides
    @Singleton
    @AnalyticsDataStore
    fun provideAnalyticsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(STORE_FILE_NAME) }

    @Provides
    @Singleton
    @AnalyticsConsentDataStore
    fun provideAnalyticsConsentDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(CONSENT_STORE_FILE_NAME) }
}
