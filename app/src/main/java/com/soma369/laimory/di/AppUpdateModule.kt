package com.soma369.laimory.di

import com.soma369.laimory.BuildConfig
import com.soma369.laimory.update.InstalledVersionCode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppUpdateModule {
    @Provides
    @Singleton
    @InstalledVersionCode
    fun provideInstalledVersionCode(): Int = BuildConfig.VERSION_CODE
}
