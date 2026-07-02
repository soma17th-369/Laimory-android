package com.soma369.laimory.core.data.di.usecase

import com.soma369.laimory.core.domain.notification.UserNotifier
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
        notifier: UserNotifier,
    ): GetFeature1ItemsUseCase = GetFeature1ItemsUseCase(repository, notifier)

    @Provides
    @Singleton
    fun provideTriggerServerErrorUseCase(
        repository: Feature1Repository,
        notifier: UserNotifier,
    ): TriggerServerErrorUseCase = TriggerServerErrorUseCase(repository, notifier)

    @Provides
    @Singleton
    fun provideTriggerUnauthorizedErrorUseCase(
        repository: Feature1Repository,
        notifier: UserNotifier,
    ): TriggerUnauthorizedErrorUseCase = TriggerUnauthorizedErrorUseCase(repository, notifier)

    @Provides
    @Singleton
    fun provideTriggerNetworkErrorUseCase(
        repository: Feature1Repository,
        notifier: UserNotifier,
    ): TriggerNetworkErrorUseCase = TriggerNetworkErrorUseCase(repository, notifier)
}
