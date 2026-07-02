package com.soma369.laimory.core.data.di.usecase

import com.soma369.laimory.core.domain.notification.MessageHelper
import com.soma369.laimory.core.domain.repository.IntroRepository
import com.soma369.laimory.core.domain.usecase.GetIntroInfoUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntroUseCaseModule {
    @Provides
    @Singleton
    fun provideGetIntroInfoUseCase(
        repository: IntroRepository,
        messageHelper: MessageHelper,
    ): GetIntroInfoUseCase = GetIntroInfoUseCase(repository, messageHelper)
}
