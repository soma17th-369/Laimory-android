package com.soma369.laimory.core.data.di.usecase

import com.soma369.laimory.core.domain.repository.Feature1Repository
import com.soma369.laimory.core.domain.usecase.GetFeature1ItemsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object Feature1UseCaseModule {
    @Provides
    fun provideGetFeature1ItemsUseCase(repository: Feature1Repository): GetFeature1ItemsUseCase = GetFeature1ItemsUseCase(repository)
}
