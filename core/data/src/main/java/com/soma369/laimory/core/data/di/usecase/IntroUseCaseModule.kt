package com.soma369.laimory.core.data.di.usecase

import com.soma369.laimory.core.domain.repository.IntroRepository
import com.soma369.laimory.core.domain.usecase.GetIntroInfoUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object IntroUseCaseModule {
    @Provides
    fun provideGetIntroInfoUseCase(repository: IntroRepository): GetIntroInfoUseCase = GetIntroInfoUseCase(repository)
}
