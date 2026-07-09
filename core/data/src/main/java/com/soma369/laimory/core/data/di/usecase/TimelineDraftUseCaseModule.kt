package com.soma369.laimory.core.data.di.usecase

import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import com.soma369.laimory.core.domain.usecase.CreateTimelineDraftUseCase
import com.soma369.laimory.core.domain.usecase.GetDraftTaskStatusUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimelineDraftUseCaseModule {
    @Provides
    @Singleton
    fun provideCreateTimelineDraftUseCase(
        repository: TimelineDraftRepository,
        messageHelper: MessageHelper,
    ): CreateTimelineDraftUseCase = CreateTimelineDraftUseCase(repository, messageHelper)

    @Provides
    @Singleton
    fun provideGetDraftTaskStatusUseCase(
        repository: TimelineDraftRepository,
        messageHelper: MessageHelper,
    ): GetDraftTaskStatusUseCase = GetDraftTaskStatusUseCase(repository, messageHelper)
}
