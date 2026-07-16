package com.soma369.laimory.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.soma369.laimory.core.data.auth.EncryptedPendingLoginStore
import com.soma369.laimory.core.data.auth.PendingLoginStore
import com.soma369.laimory.core.data.auth.PkceGenerator
import com.soma369.laimory.core.data.auth.SecurePkceGenerator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthStorageModule {
    /** 변경 시 backup_rules.xml과 data_extraction_rules.xml의 DataStore 제외 경로도 함께 변경한다. */
    const val STORE_FILE_NAME = "auth_session"

    @Provides
    @Singleton
    @AuthSessionDataStore
    fun provideAuthSessionDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(STORE_FILE_NAME) }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthStorageBindingModule {
    @Binds
    @Singleton
    abstract fun bindPendingLoginStore(impl: EncryptedPendingLoginStore): PendingLoginStore

    @Binds
    abstract fun bindPkceGenerator(impl: SecurePkceGenerator): PkceGenerator
}
