package com.soma369.laimory.di

import com.soma369.laimory.BuildConfig
import com.soma369.laimory.core.domain.coordinator.DefaultDraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.DefaultUserProfileCoordinator
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.UserProfileCoordinator
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.timeline.DraftConsentSubmissionGate
import com.soma369.laimory.core.domain.model.timeline.DraftPollingPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReporter
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import com.soma369.laimory.draft.LogcatDraftSourceItemSelectionReporter
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

    @Binds
    @Singleton
    abstract fun bindUserProfileCoordinator(impl: DefaultUserProfileCoordinator): UserProfileCoordinator

    @Binds
    @Singleton
    abstract fun bindDraftSourceItemSelectionReporter(impl: LogcatDraftSourceItemSelectionReporter): DraftSourceItemSelectionReporter
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

    @Provides
    @Singleton
    fun provideDraftSourceItemSelectionPolicy(): DraftSourceItemSelectionPolicy = DraftSourceItemSelectionPolicy()

    /**
     * 동의 문구가 법무 확정 전 임시 문구인 동안의 배포 가드 — 릴리즈 빌드에서는 실제 제출을
     * 차단하고 디버그에서만 허용한다(#231 배포 조건). 문구 확정 시 상시 허용으로 전환한다.
     */
    @Provides
    @Singleton
    fun provideDraftConsentSubmissionGate(): DraftConsentSubmissionGate = DraftConsentSubmissionGate { BuildConfig.DEBUG }
}
