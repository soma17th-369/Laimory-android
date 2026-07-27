package com.soma369.laimory.push

import com.soma369.laimory.core.domain.provider.PushInstallationIdProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PushInstallationModule {
    @Binds
    @Singleton
    abstract fun bindPushInstallationIdProvider(impl: FirebasePushInstallationIdProvider): PushInstallationIdProvider
}
