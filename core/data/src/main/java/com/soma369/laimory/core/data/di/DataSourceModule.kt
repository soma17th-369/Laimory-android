package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.datasource.remote.Feature1RemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.Feature1RemoteDataSourceImpl
import com.soma369.laimory.core.data.datasource.remote.IntroRemoteDataSource
import com.soma369.laimory.core.data.datasource.remote.IntroRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    @Singleton
    abstract fun bindFeature1RemoteDataSource(impl: Feature1RemoteDataSourceImpl): Feature1RemoteDataSource

    @Binds
    @Singleton
    abstract fun bindIntroRemoteDataSource(impl: IntroRemoteDataSourceImpl): IntroRemoteDataSource
}
