package com.soma369.laimory.core.collection.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.soma369.laimory.core.collection.notification.InstalledAppsProviderImpl
import com.soma369.laimory.core.collection.notification.NotificationFilterStore
import com.soma369.laimory.core.domain.model.collection.NotificationPrivacyPolicy
import com.soma369.laimory.core.domain.repository.NotificationFilterRepository
import com.soma369.laimory.core.domain.source.InstalledAppsProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 알림 수집 필터 저장(DataStore)과 개인정보 정책 배선. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationModule {
    @Binds
    abstract fun bindNotificationFilterRepository(impl: NotificationFilterStore): NotificationFilterRepository

    @Binds
    abstract fun bindInstalledAppsProvider(impl: InstalledAppsProviderImpl): InstalledAppsProvider

    companion object {
        @Provides
        @Singleton
        fun provideNotificationFilterDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("notification_filter") }

        /** 수집 리스너와 초안 조회 경계가 같은 정책 인스턴스를 쓰도록 단일 제공한다. */
        @Provides
        @Singleton
        fun provideNotificationPrivacyPolicy(): NotificationPrivacyPolicy = NotificationPrivacyPolicy()
    }
}
