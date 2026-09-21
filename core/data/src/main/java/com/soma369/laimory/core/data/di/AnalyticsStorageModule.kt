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
 * 제품 분석의 중복 방지 판정 저장소.
 *
 * 다른 저장소와 **파일을 나눈다.** 인증 저장소에 얹으면 로그아웃이 통째로 비울 때 "이미 보낸 이벤트"
 * 기록까지 사라져 같은 사건이 다시 나간다. 여기 담기는 것은 판정 키뿐이고 전송되지 않는다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsStorageModule {
    const val STORE_FILE_NAME = "analytics_dedupe"

    @Provides
    @Singleton
    @AnalyticsDataStore
    fun provideAnalyticsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(STORE_FILE_NAME) }
}
