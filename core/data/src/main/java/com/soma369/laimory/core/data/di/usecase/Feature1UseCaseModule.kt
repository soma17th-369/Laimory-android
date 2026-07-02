package com.soma369.laimory.core.data.di.usecase

import com.soma369.laimory.core.domain.notification.MessageHelper
import com.soma369.laimory.core.domain.repository.Feature1Repository
import com.soma369.laimory.core.domain.usecase.GetFeature1ItemsUseCase
import com.soma369.laimory.core.domain.usecase.TriggerNetworkErrorUseCase
import com.soma369.laimory.core.domain.usecase.TriggerServerErrorUseCase
import com.soma369.laimory.core.domain.usecase.TriggerUnauthorizedErrorUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Feature1UseCaseModule {
    @Provides
    @Singleton
    fun provideGetFeature1ItemsUseCase(
        repository: Feature1Repository,
        messageHelper: MessageHelper,
    ): GetFeature1ItemsUseCase = GetFeature1ItemsUseCase(repository, messageHelper)

    @Provides
    @Singleton
    fun provideTriggerServerErrorUseCase(
        repository: Feature1Repository,
        messageHelper: MessageHelper,
    ): TriggerServerErrorUseCase = TriggerServerErrorUseCase(repository, messageHelper)

    @Provides
    @Singleton
    fun provideTriggerUnauthorizedErrorUseCase(
        repository: Feature1Repository,
        messageHelper: MessageHelper,
    ): TriggerUnauthorizedErrorUseCase = TriggerUnauthorizedErrorUseCase(repository, messageHelper)

    @Provides
    @Singleton
    fun provideTriggerNetworkErrorUseCase(
        repository: Feature1Repository,
        messageHelper: MessageHelper,
    ): TriggerNetworkErrorUseCase = TriggerNetworkErrorUseCase(repository, messageHelper)
}
