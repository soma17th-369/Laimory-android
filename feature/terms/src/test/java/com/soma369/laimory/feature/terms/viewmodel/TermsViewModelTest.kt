package com.soma369.laimory.feature.terms.viewmodel

import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermRequirement
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermStageRequirement
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.PushRegistrationRepository
import com.soma369.laimory.core.domain.usecase.auth.LogoutUseCase
import com.soma369.laimory.core.domain.usecase.push.UnregisterCurrentPushInstallationUseCase
import com.soma369.laimory.feature.terms.state.TermsUiIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 이 ViewModel 은 **Activity 범위**라 로그인 약관 화면과 단계 동의 화면이 같은 인스턴스를 쓴다.
 * 아래 테스트들은 그 공유가 새는 자리를 잡는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TermsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val draftStages = listOf(TermStage.TIMELINE_FIRST_CREATE, TermStage.TIMELINE_LOCATION)
    private val sensitive = document(TermType.SENSITIVE_INFORMATION_CONSENT)
    private val location = document(TermType.LOCATION_BASED_SERVICE_TERMS)

    @Test
    fun `로그인 화면으로 돌아오면 단계 모드가 풀린다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 풀지 않으면 로그아웃·계정 전환으로 이용약관이 필요해져도 초안 동의 화면이 남고,
            // 버튼이 단계 동의를 등록하려 들어 이용약관을 끝낼 수 없다.
            val viewModel = createViewModel(FakeTermsCoordinator(pending = listOf(sensitive, location)))
            viewModel.sendIntent(TermsUiIntent.InitializeStages(draftStages))
            runCurrent()
            assertTrue(viewModel.state.value.isStageMode)

            viewModel.sendIntent(TermsUiIntent.InitializeLogin)
            runCurrent()

            val state = viewModel.state.value
            assertFalse(state.isStageMode)
            assertTrue(state.stageDocuments.isEmpty())
            assertTrue(state.checkedStageTerms.isEmpty())
        }

    @Test
    fun `단계 화면을 다시 열면 목록을 새로 읽는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // 닫았다 여는 사이에 동의가 기록됐을 수 있다. 이전 목록을 그대로 쓰면 이미 끝난
            // 항목을 다시 확인하게 된다.
            val coordinator = FakeTermsCoordinator(pending = listOf(sensitive, location))
            val viewModel = createViewModel(coordinator)
            viewModel.sendIntent(TermsUiIntent.InitializeStages(draftStages))
            runCurrent()
            assertEquals(2, viewModel.state.value.stageDocuments.size)

            coordinator.pending = listOf(location)
            viewModel.sendIntent(TermsUiIntent.InitializeStages(draftStages))
            runCurrent()

            assertEquals(listOf(location), viewModel.state.value.stageDocuments)
        }

    @Test
    fun `받을 단계 동의가 없으면 화면을 닫는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val navigationHelper = RecordingNavigationHelper()
            val viewModel = createViewModel(FakeTermsCoordinator(), navigationHelper)

            viewModel.sendIntent(TermsUiIntent.InitializeStages(draftStages))
            runCurrent()

            assertEquals(1, navigationHelper.backCount)
        }

    @Test
    fun `단계 동의를 마치면 화면을 닫는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val coordinator = FakeTermsCoordinator(pending = listOf(sensitive))
            val navigationHelper = RecordingNavigationHelper()
            val viewModel = createViewModel(coordinator, navigationHelper)
            viewModel.sendIntent(TermsUiIntent.InitializeStages(draftStages))
            runCurrent()

            viewModel.sendIntent(TermsUiIntent.StageTermToggled(sensitive.termType))
            viewModel.sendIntent(TermsUiIntent.AgreeClicked)
            runCurrent()

            assertEquals(listOf(sensitive), coordinator.agreed)
            assertEquals(1, navigationHelper.backCount)
        }

    private fun createViewModel(
        coordinator: FakeTermsCoordinator,
        navigationHelper: NavigationHelper = RecordingNavigationHelper(),
    ) = TermsViewModel(
        coordinator = coordinator,
        logout =
            LogoutUseCase(
                repository = FakeAuthRepository,
                unregisterCurrentPushInstallation =
                    UnregisterCurrentPushInstallationUseCase(
                        installationIdProvider = { "fid" },
                        repository = FakePushRegistrationRepository,
                    ),
            ),
        navigationHelper = navigationHelper,
    )

    private fun document(type: TermType) =
        TermDocument(
            termType = type,
            version = "1.0",
            title = type.name,
            contentUrl = "https://laimory.app/terms/${type.name}",
        )

    private class FakeTermsCoordinator(
        var pending: List<TermDocument> = emptyList(),
    ) : TermsAgreementCoordinator {
        val agreed = mutableListOf<TermDocument>()

        override val loginGate: StateFlow<TermsGateState> = MutableStateFlow(TermsGateState.Satisfied)

        override fun refresh() = Unit

        override suspend fun requirementOf(stage: TermStage): Result<TermStageRequirement> =
            Result.success(
                TermStageRequirement(
                    stage,
                    pending
                        .filter { it.termType in stage.requiredTypes }
                        .map { TermRequirement(it, isAgreed = false) },
                ),
            )

        override suspend fun documentOf(type: TermType): TermDocument? = pending.firstOrNull { it.termType == type }

        override suspend fun agree(documents: List<TermDocument>): Result<Unit> {
            agreed += documents
            return Result.success(Unit)
        }
    }

    private class RecordingNavigationHelper : NavigationHelper {
        var backCount = 0
            private set

        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) = Unit

        override fun navigateToBack() {
            backCount++
        }
    }

    private object FakeAuthRepository : AuthRepository {
        override fun observeSessionState(): Flow<AuthSessionState> = flowOf(AuthSessionState.Loading)

        override fun observeSignedInAccount(): Flow<SignedInAccount?> = flowOf(null)

        override suspend fun issueTokens(
            appCode: String,
            codeVerifier: String,
        ) = error("사용하지 않음")

        override suspend fun logout() = Unit

        override suspend fun clearSession() = Unit
    }

    private object FakePushRegistrationRepository : PushRegistrationRepository {
        override suspend fun register(firebaseInstallationId: String) = Unit

        override suspend fun unregister(firebaseInstallationId: String) = Unit
    }
}
