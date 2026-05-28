package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.repository.Feature1RepositoryImpl
import com.soma369.laimory.core.domain.repository.Feature1Repository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFeature1Repository(impl: Feature1RepositoryImpl): Feature1Repository
}
