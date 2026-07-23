package com.soma369.laimory.di

import com.soma369.laimory.core.domain.coordinator.DefaultDraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.timeline.DraftPollingPolicy
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
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
    fun provideApplicationCoroutineScope(): CoroutineScope {
        val exceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                Logger.e(LogDomain.DRAFT_TASK, "Unhandled draft task coroutine failure", throwable)
            }
        return CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideDraftPollingPolicy(): DraftPollingPolicy = DraftPollingPolicy()
}
