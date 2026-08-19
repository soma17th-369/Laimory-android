package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.coordinator.UserProfileCoordinator
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.provider.PushInstallationIdProvider
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.PushRegistrationRepository
import com.soma369.laimory.core.domain.usecase.auth.LogoutUseCase
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import com.soma369.laimory.core.domain.usecase.push.UnregisterCurrentPushInstallationUseCase
import com.soma369.laimory.core.domain.usecase.user.ObserveUserProfileUseCase
import com.soma369.laimory.core.domain.usecase.user.RefreshUserProfileUseCase
import com.soma369.laimory.feature.settings.state.SettingsUiIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val navigationHelper = FakeNavigationHelper()
    private val messageHelper = FakeMessageHelper()
    private val globalLoadingHelper = RecordingGlobalLoadingHelper()
    private val userProfileCoordinator = FakeUserProfileCoordinator()

    @Test
    fun `저장된 로그인 제공자를 화면 상태에 반영한다`() =
        runTest {
            repository.account.value = SignedInAccount(SocialLoginProvider.KAKAO)

            val viewModel = createViewModel()
            runCurrent()

            assertEquals(SocialLoginProvider.KAKAO, viewModel.state.value.accountProvider)
        }

    @Test
    fun `로그아웃 클릭은 공통 Dialog로 확인을 요청하고 취소 결과는 로그아웃하지 않는다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()

            val request = messageHelper.dialogRequests.single()
            assertEquals("로그아웃할까요?", request.title)
            assertEquals("로그아웃", request.primaryLabel)

            messageHelper.respond(DialogResult.Secondary)
            runCurrent()

            assertEquals(0, repository.logoutCount)
            assertFalse(viewModel.state.value.isLoggingOut)
        }

    @Test
    fun `확인 결과는 로그아웃을 실행하고 로그인 화면으로 루트를 교체한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()

            assertEquals(1, repository.logoutCount)
            assertEquals(LoginPage, navigationHelper.replacedRoot)
            assertTrue(viewModel.state.value.isLoggingOut)
            assertEquals(listOf("settings-logout"), globalLoadingHelper.startedKeys)
            assertTrue(globalLoadingHelper.activeKeys.value.isEmpty())
        }

    @Test
    fun `로그아웃 진행 동안 전역 로딩 key를 유지한다`() =
        runTest {
            repository.logoutGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()

            assertTrue(globalLoadingHelper.activeKeys.value.contains("settings-logout"))

            repository.logoutGate?.complete(Unit)
            runCurrent()

            assertTrue(globalLoadingHelper.activeKeys.value.isEmpty())
            assertEquals(LoginPage, navigationHelper.replacedRoot)
        }

    @Test
    fun `연속 탭은 확인 Dialog를 한 번만 요청한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()

            assertEquals(1, messageHelper.dialogRequests.size)
        }

    @Test
    fun `로그아웃 처리 중 중복 확인은 한 번만 실행한다`() =
        runTest {
            repository.logoutGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutConfirmed)
            runCurrent()
            assertTrue(viewModel.state.value.isLoggingOut)

            viewModel.sendIntent(SettingsUiIntent.LogoutConfirmed)
            repository.logoutGate?.complete(Unit)
            runCurrent()

            assertEquals(1, repository.logoutCount)
            assertEquals(LoginPage, navigationHelper.replacedRoot)
        }

    @Test
    fun `로그아웃 처리 중에는 확인 Dialog를 다시 요청하지 않는다`() =
        runTest {
            repository.logoutGate = CompletableDeferred()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutConfirmed)
            runCurrent()
            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            repository.logoutGate?.complete(Unit)
            runCurrent()

            assertTrue(messageHelper.dialogRequests.isEmpty())
        }

    @Test
    fun `재로그인 계정을 관찰하면 로그아웃 진행 상태를 초기화한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()
            repository.account.value = SignedInAccount(SocialLoginProvider.GOOGLE)
            runCurrent()

            assertFalse(viewModel.state.value.isLoggingOut)
            assertEquals(SocialLoginProvider.GOOGLE, viewModel.state.value.accountProvider)
        }

    @Test
    fun `로그아웃 실패는 진행 상태와 전역 로딩을 해제하고 사용자에게 안내한다`() =
        runTest {
            repository.logoutError = ApiException.NetworkException()
            val viewModel = createViewModel()
            val snackbarMessage = async { viewModel.snackbar.first() }
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            messageHelper.respond(DialogResult.Primary)
            runCurrent()

            assertEquals(1, repository.logoutCount)
            assertFalse(viewModel.state.value.isLoggingOut)
            assertTrue(globalLoadingHelper.activeKeys.value.isEmpty())
            assertNull(navigationHelper.replacedRoot)
            assertEquals(ApiException.NETWORK_ERROR, snackbarMessage.await())
        }

    @Test
    fun `닉네임을 받으면 계정 카드 제목에 반영한다`() =
        runTest {
            repository.account.value = SignedInAccount(SocialLoginProvider.KAKAO)
            val viewModel = createViewModel()
            runCurrent()

            userProfileCoordinator.emit(UserProfile.of("김소마"))
            runCurrent()

            assertEquals("김소마", viewModel.state.value.nickname)
            // 제공자 정보는 보조 문구·아이콘으로 남아야 하므로 함께 유지한다.
            assertEquals(SocialLoginProvider.KAKAO, viewModel.state.value.accountProvider)
        }

    @Test
    fun `닉네임이 없는 계정은 상태를 비워 제공자 문구로 돌아가게 한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            userProfileCoordinator.emit(UserProfile.of(null))
            runCurrent()

            assertNull(viewModel.state.value.nickname)
        }

    @Test
    fun `로그아웃으로 공용 프로필이 비면 닉네임도 사라진다`() =
        runTest {
            repository.account.value = SignedInAccount(SocialLoginProvider.GOOGLE)
            val viewModel = createViewModel()
            runCurrent()
            userProfileCoordinator.emit(UserProfile.of("김소마"))
            runCurrent()

            userProfileCoordinator.emit(null)
            runCurrent()

            // 설정 화면에 이전 계정 정보가 남으면 안 된다.
            assertNull(viewModel.state.value.nickname)
        }

    @Test
    fun `화면이 뜰 때마다 아직 못 받은 프로필을 다시 요청한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()
            assertEquals(0, userProfileCoordinator.refreshCount)

            // 재진입해도 같은 ViewModel 인스턴스라 init 트리거로는 다시 시도하지 못한다.
            viewModel.sendIntent(SettingsUiIntent.RefreshProfile)
            runCurrent()
            assertEquals(1, userProfileCoordinator.refreshCount)

            viewModel.sendIntent(SettingsUiIntent.RefreshProfile)
            runCurrent()

            assertEquals(2, userProfileCoordinator.refreshCount)
        }

    /** 공용 프로필 상태를 시험에서 직접 밀어 넣는 대역. */
    private class FakeUserProfileCoordinator : UserProfileCoordinator {
        private val mutableProfile = MutableStateFlow<UserProfile?>(null)

        var refreshCount = 0
            private set

        override val profile: StateFlow<UserProfile?> = mutableProfile

        override fun refresh() {
            refreshCount++
        }

        fun emit(profile: UserProfile?) {
            mutableProfile.value = profile
        }
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            logoutUseCase =
                LogoutUseCase(
                    repository = repository,
                    unregisterCurrentPushInstallation =
                        UnregisterCurrentPushInstallationUseCase(
                            installationIdProvider = FakePushInstallationIdProvider,
                            repository = FakePushRegistrationRepository,
                        ),
                ),
            observeSignedInAccount = ObserveSignedInAccountUseCase(repository),
            observeUserProfileUseCase = ObserveUserProfileUseCase(userProfileCoordinator),
            refreshUserProfileUseCase = RefreshUserProfileUseCase(userProfileCoordinator),
            navigationHelper = navigationHelper,
            messageHelper = messageHelper,
            globalLoadingHelper = globalLoadingHelper,
        )

    private class FakeMessageHelper : MessageHelper {
        val dialogRequests = mutableListOf<DialogRequest.TwoButton>()
        private var pendingResponse: CompletableDeferred<DialogResult>? = null

        override fun send(message: UserMessage) = Unit

        override suspend fun showTwoButtonDialog(request: DialogRequest.TwoButton): DialogResult {
            dialogRequests += request
            val response = CompletableDeferred<DialogResult>()
            pendingResponse = response
            return response.await()
        }

        fun respond(result: DialogResult) {
            val response = checkNotNull(pendingResponse) { "대기 중인 Dialog 요청이 없습니다" }
            pendingResponse = null
            response.complete(result)
        }
    }

    private class RecordingGlobalLoadingHelper : GlobalLoadingHelper {
        private val _activeKeys = MutableStateFlow<Set<String>>(emptySet())
        override val activeKeys: StateFlow<Set<String>> = _activeKeys.asStateFlow()
        val startedKeys = mutableListOf<String>()

        override suspend fun <T> withLoading(
            key: String,
            block: suspend () -> T,
        ): T {
            startedKeys += key
            _activeKeys.value = _activeKeys.value + key
            return try {
                block()
            } finally {
                _activeKeys.value = _activeKeys.value - key
            }
        }
    }

    private class FakeAuthRepository : AuthRepository {
        val account = MutableStateFlow<SignedInAccount?>(null)
        var logoutCount = 0
        var logoutGate: CompletableDeferred<Unit>? = null
        var logoutError: Throwable? = null

        override fun observeSessionState(): Flow<AuthSessionState> = MutableStateFlow(AuthSessionState.Authenticated)

        override fun observeSignedInAccount(): Flow<SignedInAccount?> = account

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) = Unit

        override suspend fun logout() {
            logoutCount++
            logoutError?.let { throw it }
            logoutGate?.await()
            account.value = null
        }
    }

    private class FakeNavigationHelper : NavigationHelper {
        var replacedRoot: Page? = null

        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) {
            replacedRoot = page
        }

        override fun navigateToBack() = Unit
    }

    private data object FakePushInstallationIdProvider : PushInstallationIdProvider {
        override suspend fun getCurrentId(): String = "fid"
    }

    private data object FakePushRegistrationRepository : PushRegistrationRepository {
        override suspend fun unregister(firebaseInstallationId: String) = Unit

        override suspend fun register(firebaseInstallationId: String) = Unit
    }
}
