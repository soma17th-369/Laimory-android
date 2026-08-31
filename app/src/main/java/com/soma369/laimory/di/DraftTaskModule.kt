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
import com.soma369.laimory.core.domain.model.timeline.DraftConsentSubmissionGate
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
     * 동의 문구가 법무 확정 전 임시 문구인 동안의 배포 가드 — 릴리즈 빌드에서는 실제 제출을
     * 차단하고 디버그에서만 허용한다(#231 배포 조건). 문구 확정 시 상시 허용으로 전환한다.
     */
    @Provides
    @Singleton
    fun provideDraftConsentSubmissionGate(): DraftConsentSubmissionGate = DraftConsentSubmissionGate { BuildConfig.DEBUG }

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
     * 정본은 계정 단위 최초 1회 동의(#238)인데 그 저장소가 아직 없다. 그때까지는 제출 게이트와
     * 같은 방식으로 debug 빌드에서만 열어 둔다 — 실기기로 지도·마커를 확인할 수 있어야 하고,
     * 릴리즈 사용자에게는 동의 없이 지도가 뜨지 않는다. #238 이 들어오면 저장된 동의를 읽도록 바꾼다.
     *
     * API 키가 비어 있으면 SDK 인증이 실패하므로 아예 붙이지 않고 대체 안내로 넘긴다.
     */
    @Provides
    @Singleton
    fun provideLocationMapRenderGate(): LocationMapRenderGate =
        LocationMapRenderGate { BuildConfig.DEBUG && BuildConfig.MAPS_API_KEY.isNotBlank() }
}
