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
 * 제품 분석 저장소 — 중복 방지 판정 키, 완료 전 편집 흔적, 설치 유입 귀속.
 *
 * 다른 저장소와 **파일을 나눈다.** 인증 저장소에 얹으면 로그아웃이 통째로 비울 때 "이미 보낸 이벤트"
 * 기록까지 사라져 같은 사건이 다시 나간다. 여기 담기는 것은 기기 안의 판정 재료뿐이고 전송되지 않는다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AnalyticsStorageModule {
    const val STORE_FILE_NAME = "analytics_dedupe"

    /** 완료 전 편집 흔적. 판정 키와 수명·모양이 달라(기록 완료 때 비움) 파일을 나눈다. */
    const val TIMELINE_EDIT_FILE_NAME = "analytics_timeline_edits"

    /**
     * 설치 유입 귀속. 설치에 딸린 값이라 로그아웃이 비우지 않고, **백업에서 제외한다**(`backup_rules.xml`·
     * `data_extraction_rules.xml`). 재설치 뒤 되살아나면 새 설치의 Referrer 를 영영 읽지 않는다.
     */
    const val INSTALL_ATTRIBUTION_FILE_NAME = "analytics_install_attribution"

    @Provides
    @Singleton
    @AnalyticsDataStore
    fun provideAnalyticsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(STORE_FILE_NAME) }

    @Provides
    @Singleton
    @AnalyticsTimelineEditDataStore
    fun provideAnalyticsTimelineEditDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(TIMELINE_EDIT_FILE_NAME) }

    @Provides
    @Singleton
    @AnalyticsInstallAttributionDataStore
    fun provideAnalyticsInstallAttributionDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(INSTALL_ATTRIBUTION_FILE_NAME) }
}
