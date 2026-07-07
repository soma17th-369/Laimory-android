package com.soma369.laimory.core.collection.di

import com.soma369.laimory.core.collection.repository.SourceItemRepositoryImpl
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CollectionRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSourceItemRepository(impl: SourceItemRepositoryImpl): SourceItemRepository
}
