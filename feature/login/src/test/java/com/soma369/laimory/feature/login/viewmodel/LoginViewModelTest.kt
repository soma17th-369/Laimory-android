package com.soma369.laimory.feature.login.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.helper.SocialLoginCallbackHandler
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.auth.SocialLoginAttempt
import com.soma369.laimory.core.domain.model.auth.SocialLoginCallback
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.SocialLoginRepository
import com.soma369.laimory.core.domain.usecase.auth.CancelSocialLoginUseCase
import com.soma369.laimory.core.domain.usecase.auth.CompleteSocialLoginUseCase
import com.soma369.laimory.core.domain.usecase.auth.IssueAuthTokensUseCase
import com.soma369.laimory.core.domain.usecase.auth.StartSocialLoginUseCase
import com.soma369.laimory.feature.login.state.LoginPhase
import com.soma369.laimory.feature.login.state.LoginUiIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val socialRepository = FakeSocialLoginRepository()
    private val authRepository = FakeAuthRepository()
    private val callbackHandler = FakeCallbackHandler()
    private val navigationHelper = FakeNavigationHelper()

    @Test
    fun `제공자 버튼은 한 번만 시도를 만들고 Custom Tab 주소를 연다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            val effect = backgroundScope.launch { viewModel.sideEffect.first() }

            viewModel.sendIntent(LoginUiIntent.ProviderClicked(SocialLoginProvider.GOOGLE))
            viewModel.sendIntent(LoginUiIntent.ProviderClicked(SocialLoginProvider.GOOGLE))
            runCurrent()

            assertEquals(1, socialRepository.startCount)
            assertEquals(LoginPhase.WAITING_CALLBACK, viewModel.state.value.phase)
            assertTrue(effect.isActive.not())
        }

    @Test
    fun `정상 callback은 token을 발급하고 Home으로 루트를 교체한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            socialRepository.pendingVerifier = "verifier"
            val viewModel = createViewModel()
            runCurrent()

            callbackHandler.handle(SocialLoginCallback(appCode = "code"))
            runCurrent()

            assertEquals("code", authRepository.appCode)
            assertEquals("verifier", authRepository.appVerifier)
            assertEquals(HomePage, navigationHelper.replacedRoot)
            assertEquals(LoginPhase.IDLE, viewModel.state.value.phase)
        }

    @Test
    fun `callback 없이 브라우저에서 돌아오면 pending 시도를 폐기하고 재활성화한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(LoginUiIntent.ProviderClicked(SocialLoginProvider.KAKAO))
            runCurrent()

            viewModel.sendIntent(LoginUiIntent.BrowserReturnedWithoutCallback)
            runCurrent()
            advanceTimeBy(501)
            runCurrent()

            assertEquals(1, socialRepository.clearCount)
            assertEquals(LoginPhase.IDLE, viewModel.state.value.phase)
        }

    @Test
    fun `브라우저 복귀 직후 callback이 도착하면 pending 시도를 소비해 로그인을 완료한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(LoginUiIntent.ProviderClicked(SocialLoginProvider.KAKAO))
            runCurrent()

            viewModel.sendIntent(LoginUiIntent.BrowserReturnedWithoutCallback)
            callbackHandler.handle(SocialLoginCallback(appCode = "code"))
            runCurrent()
            advanceTimeBy(501)
            runCurrent()

            assertEquals(0, socialRepository.clearCount)
            assertEquals("code", authRepository.appCode)
            assertEquals("new-verifier", authRepository.appVerifier)
            assertEquals(HomePage, navigationHelper.replacedRoot)
            assertEquals(LoginPhase.IDLE, viewModel.state.value.phase)
        }

    @Test
    fun `브라우저를 열 수 없으면 pending 시도를 폐기하고 오류를 표시한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.sendIntent(LoginUiIntent.ProviderClicked(SocialLoginProvider.GOOGLE))
            runCurrent()

            viewModel.sendIntent(LoginUiIntent.AuthorizationLaunchFailed)
            runCurrent()

            assertEquals(1, socialRepository.clearCount)
            assertEquals(LoginPhase.IDLE, viewModel.state.value.phase)
            assertEquals("로그인을 진행할 브라우저를 찾을 수 없습니다.", viewModel.state.value.errorMessage)
        }

    @Test
    fun `활성 시도 없이 배달된 오래된 callback은 오류 없이 폐기한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            callbackHandler.handle(SocialLoginCallback(appCode = "stale-code"))
            runCurrent()

            assertEquals(LoginPhase.IDLE, viewModel.state.value.phase)
            assertNull(viewModel.state.value.errorMessage)
            assertNull(authRepository.appCode)
        }

    private fun createViewModel(): LoginViewModel =
        LoginViewModel(
            startSocialLogin = StartSocialLoginUseCase(socialRepository),
            completeSocialLogin =
                CompleteSocialLoginUseCase(
                    socialRepository,
                    IssueAuthTokensUseCase(authRepository),
                ),
            cancelSocialLogin = CancelSocialLoginUseCase(socialRepository),
            callbackHandler = callbackHandler,
            navigationHelper = navigationHelper,
        )

    private class FakeSocialLoginRepository : SocialLoginRepository {
        var startCount = 0
        var clearCount = 0
        var pendingVerifier: String? = null

        override suspend fun start(provider: SocialLoginProvider): SocialLoginAttempt {
            startCount++
            pendingVerifier = "new-verifier"
            return SocialLoginAttempt("https://dev.laimory.app/oauth2/authorization/${provider.name.lowercase()}")
        }

        override suspend fun consumePendingVerifier(): String? = pendingVerifier.also { pendingVerifier = null }

        override suspend fun clearPendingAttempt() {
            clearCount++
            pendingVerifier = null
        }
    }

    private class FakeAuthRepository : AuthRepository {
        var appCode: String? = null
        var appVerifier: String? = null

        override fun observeSessionState(): Flow<AuthSessionState> = emptyFlow()

        override fun observeSignedInAccount(): Flow<SignedInAccount?> = emptyFlow()

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) {
            this.appCode = appCode
            this.appVerifier = appVerifier
        }

        override suspend fun logout() = Unit

        override suspend fun clearSession() = Unit
    }

    private class FakeCallbackHandler : SocialLoginCallbackHandler {
        private val flow = MutableSharedFlow<SocialLoginCallback>(extraBufferCapacity = 1)
        override val callbacks: Flow<SocialLoginCallback> = flow

        override fun handle(callback: SocialLoginCallback) {
            flow.tryEmit(callback)
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
}
