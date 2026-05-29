package com.soma369.laimory.core.data.di.usecase

import com.soma369.laimory.core.domain.repository.Feature1Repository
import com.soma369.laimory.core.domain.usecase.GetFeature1ItemsUseCase
import com.soma369.laimory.core.domain.usecase.TriggerNetworkErrorUseCase
import com.soma369.laimory.core.domain.usecase.TriggerServerErrorUseCase
import com.soma369.laimory.core.domain.usecase.TriggerUnauthorizedErrorUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object Feature1UseCaseModule {
    @Provides
    fun provideGetFeature1ItemsUseCase(repository: Feature1Repository): GetFeature1ItemsUseCase = GetFeature1ItemsUseCase(repository)

    @Provides
    fun provideTriggerServerErrorUseCase(repository: Feature1Repository): TriggerServerErrorUseCase = TriggerServerErrorUseCase(repository)

    @Provides
    fun provideTriggerUnauthorizedErrorUseCase(repository: Feature1Repository): TriggerUnauthorizedErrorUseCase =
        TriggerUnauthorizedErrorUseCase(repository)

    @Provides
    fun provideTriggerNetworkErrorUseCase(repository: Feature1Repository): TriggerNetworkErrorUseCase =
        TriggerNetworkErrorUseCase(repository)
}
