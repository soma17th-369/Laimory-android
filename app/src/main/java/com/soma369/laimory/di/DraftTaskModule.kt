package com.soma369.laimory.di

import com.soma369.laimory.BuildConfig
import com.soma369.laimory.core.domain.coordinator.AutoCollectionCoordinator
import com.soma369.laimory.core.domain.coordinator.DefaultAutoCollectionCoordinator
import com.soma369.laimory.core.domain.coordinator.DefaultDraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.DefaultOnboardingCompletionCoordinator
import com.soma369.laimory.core.domain.coordinator.DefaultTermsAgreementCoordinator
import com.soma369.laimory.core.domain.coordinator.DefaultUserProfileCoordinator
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.coordinator.OnboardingCompletionCoordinator
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.coordinator.UserProfileCoordinator
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.collection.CollectionLabAccessGate
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.timeline.DraftPollingPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReporter
import com.soma369.laimory.core.domain.model.timeline.LocationMapRenderGate
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

    /** 온보딩 완료 여부를 세션당 한 번 조회해 앱 루트가 나눠 쓰는 조율자. */
    @Binds
    @Singleton
    abstract fun bindOnboardingCompletionCoordinator(impl: DefaultOnboardingCompletionCoordinator): OnboardingCompletionCoordinator

    /** 약관 catalog·동의 이력을 세션당 한 번 읽어 루트 gate 와 초안 생성 동의가 나눠 쓰는 조율자. */
    @Binds
    @Singleton
    abstract fun bindTermsAgreementCoordinator(impl: DefaultTermsAgreementCoordinator): TermsAgreementCoordinator

    /** 일정·건강 자동 수집을 앱 전경 진입·초안 설정·최종 생성이 함께 쓰는 조율자. */
    @Binds
    @Singleton
    abstract fun bindAutoCollectionCoordinator(impl: DefaultAutoCollectionCoordinator): AutoCollectionCoordinator

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
     * 수집 실험실 접근 허용 여부. 개발 도구라 debug 에서만 연다.
     *
     * 자동 수집과 무관하다 — 릴리즈에서도 권한이 있는 일정·건강 자동 수집은 그대로 돈다.
     */
    @Provides
    @Singleton
    fun provideCollectionLabAccessGate(): CollectionLabAccessGate = CollectionLabAccessGate { BuildConfig.DEBUG }

    /**
     * 지도 렌더링 허용 여부.
     *
     * 빌드 타입으로 가르지 않는다. 지도를 그릴 때 나가는 것은 사용자의 위치이지 개발용 기능이
     * 아니므로, 판정은 **저장된 위치정보 약관 동의**여야 한다. 온보딩이 이 단계를 필수로 받으므로
     * 정상 경로로 들어온 사용자는 이미 동의한 상태다.
     *
     * catalog 가 비어 있으면 요구가 없어 만족으로 본다 — 서버의 fail-open 과 같은 판정이다.
     * 반대로 조회에 실패하면 동의 여부를 모르는 것이므로 그리지 않는다.
     *
     * API 키가 비어 있으면 SDK 인증이 실패하므로 아예 붙이지 않고 대체 안내로 넘긴다. 키 조회가
     * 먼저라 키가 없는 환경에서는 약관 조회를 하지 않는다.
     */
    @Provides
    @Singleton
    fun provideLocationMapRenderGate(termsCoordinator: TermsAgreementCoordinator): LocationMapRenderGate =
        LocationMapRenderGate {
            BuildConfig.MAPS_API_KEY.isNotBlank() &&
                termsCoordinator.requirementOf(TermStage.TIMELINE_LOCATION).getOrNull()?.isSatisfied == true
        }
}
