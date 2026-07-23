package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.repository.ActiveDraftTaskRepositoryImpl
import com.soma369.laimory.core.data.repository.AuthRepositoryImpl
import com.soma369.laimory.core.data.repository.Feature1RepositoryImpl
import com.soma369.laimory.core.data.repository.IntroRepositoryImpl
import com.soma369.laimory.core.data.repository.SocialLoginRepositoryImpl
import com.soma369.laimory.core.data.repository.TimelineDraftRepositoryImpl
import com.soma369.laimory.core.data.repository.TimelineRecordSessionRepositoryImpl
import com.soma369.laimory.core.domain.repository.ActiveDraftTaskRepository
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.Feature1Repository
import com.soma369.laimory.core.domain.repository.IntroRepository
import com.soma369.laimory.core.domain.repository.SocialLoginRepository
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
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
    internal abstract fun bindActiveDraftTaskRepository(impl: ActiveDraftTaskRepositoryImpl): ActiveDraftTaskRepository

    @Binds
    @Singleton
    internal abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    internal abstract fun bindSocialLoginRepository(impl: SocialLoginRepositoryImpl): SocialLoginRepository

    @Binds
    @Singleton
    abstract fun bindFeature1Repository(impl: Feature1RepositoryImpl): Feature1Repository

    @Binds
    @Singleton
    abstract fun bindIntroRepository(impl: IntroRepositoryImpl): IntroRepository

    @Binds
    @Singleton
    abstract fun bindTimelineDraftRepository(impl: TimelineDraftRepositoryImpl): TimelineDraftRepository

    @Binds
    @Singleton
    abstract fun bindTimelineRecordSession(impl: TimelineRecordSessionRepositoryImpl): TimelineRecordSessionRepository
}
