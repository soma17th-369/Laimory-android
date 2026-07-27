package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.provider.PushInstallationIdProvider
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.PushRegistrationRepository
import com.soma369.laimory.core.domain.usecase.auth.LogoutUseCase
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import com.soma369.laimory.core.domain.usecase.push.UnregisterCurrentPushInstallationUseCase
import com.soma369.laimory.feature.settings.state.SettingsUiIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    @Test
    fun `저장된 로그인 제공자를 화면 상태에 반영한다`() =
        runTest {
            repository.account.value = SignedInAccount(SocialLoginProvider.KAKAO)

            val viewModel = createViewModel()
            runCurrent()

            assertEquals(SocialLoginProvider.KAKAO, viewModel.state.value.accountProvider)
        }

    @Test
    fun `로그아웃 선택과 취소는 확인 다이얼로그 상태만 변경한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            runCurrent()
            assertTrue(viewModel.state.value.isLogoutDialogVisible)

            viewModel.sendIntent(SettingsUiIntent.LogoutDismissed)
            runCurrent()
            assertFalse(viewModel.state.value.isLogoutDialogVisible)
            assertEquals(0, repository.logoutCount)
        }

    @Test
    fun `로그아웃 확인은 세션을 제거하고 로그인 화면으로 루트를 교체한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            viewModel.sendIntent(SettingsUiIntent.LogoutConfirmed)
            runCurrent()

            assertEquals(1, repository.logoutCount)
            assertEquals(LoginPage, navigationHelper.replacedRoot)
            assertTrue(viewModel.state.value.isLoggingOut)
            assertFalse(viewModel.state.value.isLogoutDialogVisible)
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
            assertFalse(viewModel.state.value.isLogoutDialogVisible)
        }

    @Test
    fun `재로그인 계정을 관찰하면 로그아웃 진행 상태를 초기화한다`() =
        runTest {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            viewModel.sendIntent(SettingsUiIntent.LogoutConfirmed)
            runCurrent()
            repository.account.value = SignedInAccount(SocialLoginProvider.GOOGLE)
            runCurrent()

            assertFalse(viewModel.state.value.isLogoutDialogVisible)
            assertFalse(viewModel.state.value.isLoggingOut)
            assertEquals(SocialLoginProvider.GOOGLE, viewModel.state.value.accountProvider)
        }

    @Test
    fun `로그아웃 실패는 진행 상태를 해제하고 사용자에게 안내한다`() =
        runTest {
            repository.logoutError = ApiException.NetworkException()
            val viewModel = createViewModel()
            val snackbarMessage = async { viewModel.snackbar.first() }
            runCurrent()

            viewModel.sendIntent(SettingsUiIntent.LogoutClicked)
            viewModel.sendIntent(SettingsUiIntent.LogoutConfirmed)
            runCurrent()

            assertEquals(1, repository.logoutCount)
            assertFalse(viewModel.state.value.isLogoutDialogVisible)
            assertFalse(viewModel.state.value.isLoggingOut)
            assertNull(navigationHelper.replacedRoot)
            assertEquals(ApiException.NETWORK_ERROR, snackbarMessage.await())
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
            navigationHelper = navigationHelper,
        )

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
        override suspend fun register(firebaseInstallationId: String) = Unit

        override suspend fun unregister(firebaseInstallationId: String) = Unit
    }
}
