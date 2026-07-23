package com.soma369.laimory.di

import com.soma369.laimory.core.domain.coordinator.DefaultDraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.timeline.DraftPollingPolicy
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DraftTaskBindingModule {
    @Binds
    @Singleton
    abstract fun bindDraftTaskCoordinator(impl: DefaultDraftTaskCoordinator): DraftTaskCoordinator
}

@Module
@InstallIn(SingletonComponent::class)
object DraftTaskRuntimeModule {
    @Provides
    @Singleton
    @ApplicationCoroutineScope
    fun provideApplicationCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideDraftPollingPolicy(): DraftPollingPolicy = DraftPollingPolicy()
}
