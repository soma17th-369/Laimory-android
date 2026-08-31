package com.soma369.laimory.feature.onboarding.viewmodel

import com.soma369.laimory.core.domain.coordinator.OnboardingCompletionCoordinator
import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.coordinator.UserProfileCoordinator
import com.soma369.laimory.core.domain.exception.StaleTermVersionException
import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.model.terms.TermAgreement
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermRequirement
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermStageRequirement
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import com.soma369.laimory.core.domain.repository.OnboardingRepository
import com.soma369.laimory.core.domain.repository.TermsRepository
import com.soma369.laimory.core.domain.usecase.CompleteOnboardingUseCase
import com.soma369.laimory.core.domain.usecase.ObserveOnboardingProgressUseCase
import com.soma369.laimory.core.domain.usecase.SaveOnboardingProgressUseCase
import com.soma369.laimory.core.domain.usecase.SetLocationTrackingUseCase
import com.soma369.laimory.core.domain.usecase.terms.GetDisplayTermsUseCase
import com.soma369.laimory.core.domain.usecase.user.ObserveUserProfileUseCase
import com.soma369.laimory.feature.onboarding.state.OnboardingUiIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingConsentTest {
    private val sensitive = document(TermType.SENSITIVE_INFORMATION_CONSENT)
    private val thirdParty = document(TermType.THIRD_PARTY_PROVISION_CONSENT)
    private val crossBorder = document(TermType.CROSS_BORDER_TRANSFER_CONSENT)
    private val location = document(TermType.LOCATION_BASED_SERVICE_TERMS)
    private val allFour = listOf(sensitive, thirdParty, crossBorder, location)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `마지막 장에서 위치약관까지 네 종류를 받는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 온보딩 시점에는 앞으로 무엇을 보낼지 알 수 없어 위치 조건을 판정할 수 없다.
            // 미리 받아 두면 초안 생성 화면이 다시 묻지 않는다.
            val viewModel = createViewModel(FakeTermsCoordinator(pending = allFour))

            runCurrent()

            assertEquals(allFour, viewModel.state.value.consentDocuments)
            assertTrue(viewModel.state.value.pages.last().showsConsents)
        }

    @Test
    fun `이미 동의했으면 목록만 비고 마지막 장은 남는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 마지막 장은 동의와 무관하게 온보딩을 끝내는 자리라 사라지면 안 된다.
            val viewModel = createViewModel(FakeTermsCoordinator(pending = emptyList()))

            runCurrent()

            assertTrue(viewModel.state.value.consentDocuments.isEmpty())
            assertTrue(viewModel.state.value.pages.last().showsConsents)
        }

    @Test
    fun `조회에 실패해도 온보딩을 막지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 동의는 초안 생성 화면이 다시 받는다. 여기서 막아 온보딩을 못 끝내게 할 이유가 없다.
            val viewModel = createViewModel(FakeTermsCoordinator(failure = IllegalStateException("offline")))

            runCurrent()

            assertTrue(viewModel.state.value.consentDocuments.isEmpty())
        }

    @Test
    fun `시작 버튼이 체크를 채운 뒤 동의를 기록한다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 항목마다 체크를 받지 않는다. 결과가 분명한 버튼 쪽이 의사가 또렷하다.
            val coordinator = FakeTermsCoordinator(pending = allFour)
            val viewModel = createViewModel(coordinator)
            runCurrent()

            assertTrue(viewModel.state.value.checkedConsents.isEmpty())

            viewModel.sendIntent(OnboardingUiIntent.Complete)
            advanceUntilIdle()

            assertEquals(allFour.map { it.termType }.toSet(), viewModel.state.value.checkedConsents)
            assertEquals(allFour, coordinator.agreed)
        }

    @Test
    fun `모두 확인하고 시작하면 동의를 기록한 뒤 완료한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val coordinator = FakeTermsCoordinator(pending = allFour)
            val viewModel = createViewModel(coordinator)
            runCurrent()
            viewModel.sendIntent(OnboardingUiIntent.Complete)
            advanceUntilIdle()

            assertEquals(allFour, coordinator.agreed)
            assertNull(viewModel.state.value.consentErrorMessage)
        }

    @Test
    fun `동의 기록이 실패하면 온보딩을 끝내지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            val coordinator = FakeTermsCoordinator(pending = allFour, agreeFailure = IllegalStateException("offline"))
            val viewModel = createViewModel(coordinator)
            runCurrent()
            viewModel.sendIntent(OnboardingUiIntent.Complete)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isCompleting)
            assertFalse(viewModel.state.value.consentErrorMessage.isNullOrBlank())
        }

    @Test
    fun `개정 경쟁이면 확인 상태를 모두 되돌리고 다시 받는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 새 버전으로 자동 재시도하면 사용자가 읽지 않은 내용에 동의한 기록이 남는다.
            val revised = listOf(document(TermType.SENSITIVE_INFORMATION_CONSENT, version = "2.0"))
            val coordinator =
                FakeTermsCoordinator(pending = allFour, agreeFailure = StaleTermVersionException(), revised = revised)
            val viewModel = createViewModel(coordinator)
            runCurrent()
            viewModel.sendIntent(OnboardingUiIntent.Complete)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.checkedConsents.isEmpty())
            assertEquals(revised, viewModel.state.value.consentDocuments)
            assertFalse(viewModel.state.value.consentErrorMessage.isNullOrBlank())
        }

    @Test
    fun `이 환경 catalog 가 비면 게시된 정본을 보여 주되 등록하지는 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 그 환경 DB 에 없는 행을 보내면 전부 거절되고, 서버도 catalog 가 없는 단계는
            // 강제하지 않는다. 화면만 채우고 아무것도 보내지 않는 편이 서버 판정과 맞는다.
            val coordinator = FakeTermsCoordinator(pending = emptyList())
            val viewModel =
                createViewModel(coordinator, displayTerms = PublishedTermsRepository(allFour))
            runCurrent()
            assertEquals(allFour, viewModel.state.value.consentDocuments)

            viewModel.sendIntent(OnboardingUiIntent.Complete)
            advanceUntilIdle()

            assertTrue(coordinator.agreed.isEmpty())
        }

    private fun createViewModel(
        coordinator: TermsAgreementCoordinator,
        displayTerms: TermsRepository = EmptyTermsRepository,
    ) = OnboardingViewModel(
        observeOnboardingProgressUseCase = ObserveOnboardingProgressUseCase(FakeOnboardingRepository),
        observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileCoordinator),
        saveOnboardingProgressUseCase = SaveOnboardingProgressUseCase(FakeOnboardingRepository),
        completeOnboardingUseCase = CompleteOnboardingUseCase(FakeOnboardingCompletionCoordinator),
        setLocationTrackingUseCase = SetLocationTrackingUseCase(FakeLocationTrackingRepository),
        termsCoordinator = coordinator,
        getDisplayTerms = GetDisplayTermsUseCase(displayTerms),
    )

    private fun document(
        type: TermType,
        version: String = "1.0",
    ) = TermDocument(
        termType = type,
        version = version,
        title = type.name,
        contentUrl = "https://laimory.app/terms/${type.name}/$version",
        effectiveAt = LocalDateTime.of(2026, 8, 28, 0, 0),
    )

    private class FakeTermsCoordinator(
        private val pending: List<TermDocument> = emptyList(),
        private val failure: Throwable? = null,
        private val agreeFailure: Throwable? = null,
        private val revised: List<TermDocument> = emptyList(),
    ) : TermsAgreementCoordinator {
        val agreed = mutableListOf<TermDocument>()
        private var hasFailedOnce = false

        override val loginGate: StateFlow<TermsGateState> = MutableStateFlow(TermsGateState.Satisfied)

        override fun refresh() = Unit

        override suspend fun requirementOf(stage: TermStage): Result<TermStageRequirement> {
            failure?.let { return Result.failure(it) }
            val documents = (if (hasFailedOnce) revised else pending).filter { it.termType in stage.requiredTypes }
            return Result.success(
                TermStageRequirement(stage, documents.map { TermRequirement(it, isAgreed = false) }),
            )
        }

        override suspend fun documentOf(type: TermType): TermDocument? = pending.firstOrNull { it.termType == type }

        override suspend fun agree(documents: List<TermDocument>): Result<Unit> {
            agreeFailure?.let {
                hasFailedOnce = true
                return Result.failure(it)
            }
            agreed += documents
            return Result.success(Unit)
        }
    }

    /** 이 환경에는 문서가 없고 게시된 정본에만 있는 상태. */
    private class PublishedTermsRepository(
        private val published: List<TermDocument>,
    ) : TermsRepository {
        override suspend fun getCurrentTerms(types: List<TermType>) = emptyList<TermDocument>()

        override suspend fun getPublishedTerms(types: List<TermType>) = published.filter { it.termType in types }

        override suspend fun getMyAgreements() = emptyList<TermAgreement>()

        override suspend fun agree(documents: List<TermDocument>) = Unit
    }

    /** catalog 가 있는 환경을 가정한다 — 대체 조회는 돌지 않는다. */
    private object EmptyTermsRepository : TermsRepository {
        override suspend fun getCurrentTerms(types: List<TermType>) = emptyList<TermDocument>()

        override suspend fun getPublishedTerms(types: List<TermType>) = emptyList<TermDocument>()

        override suspend fun getMyAgreements() = emptyList<TermAgreement>()

        override suspend fun agree(documents: List<TermDocument>) = Unit
    }

    private object FakeOnboardingRepository : OnboardingRepository {
        override suspend fun cachedCompletion(): Boolean? = null

        override suspend fun cacheCompletion(isCompleted: Boolean) = Unit

        override suspend fun recordCompletion() = Unit

        override suspend fun isCompletionPending(): Boolean = false

        override suspend fun setCompletionPending(isPending: Boolean) = Unit

        override suspend fun fetchCompletion(): Result<Boolean> = Result.success(false)

        override fun observeLastPageKey(): Flow<String?> = flowOf(null)

        override suspend fun saveProgress(pageKey: String) = Unit

        override suspend fun clear() = Unit
    }

    private object FakeUserProfileCoordinator : UserProfileCoordinator {
        override val profile: StateFlow<UserProfile?> = MutableStateFlow(null)

        override fun refresh() = Unit
    }

    private object FakeOnboardingCompletionCoordinator : OnboardingCompletionCoordinator {
        override val completed: StateFlow<Boolean?> = MutableStateFlow(false)

        override fun refresh() = Unit

        override suspend fun markCompleted() = Unit

        override suspend fun resetForCurrentSession() = Unit
    }

    private object FakeLocationTrackingRepository : LocationTrackingRepository {
        override fun observeEnabled(): Flow<Boolean> = flowOf(false)

        override fun observeStatus(): Flow<LocationTrackingStatus?> = flowOf(null)

        override suspend fun setEnabled(enabled: Boolean) = Unit
    }
}
