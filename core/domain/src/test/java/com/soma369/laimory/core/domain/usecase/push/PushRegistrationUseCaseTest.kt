package com.soma369.laimory.core.domain.usecase.push

import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.PushRegistrationRepository
import com.soma369.laimory.core.domain.usecase.auth.LogoutUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushRegistrationUseCaseTest {
    @Test
    fun `등록 UseCase는 FID 원문을 repository에 전달한다`() =
        runTest {
            val repository = FakePushRegistrationRepository()

            val result = RegisterPushInstallationUseCase(repository)("opaque-fid")

            assertTrue(result.isSuccess)
            assertEquals(listOf("opaque-fid"), repository.registered)
        }

    @Test
    fun `현재 설치 해제 UseCase는 provider의 FID로 해제한다`() =
        runTest {
            val repository = FakePushRegistrationRepository()
            val useCase =
                UnregisterCurrentPushInstallationUseCase(
                    installationIdProvider = { "current-fid" },
                    repository = repository,
                )

            val result = useCase()

            assertTrue(result.isSuccess)
            assertEquals(listOf("current-fid"), repository.unregistered)
        }

    @Test
    fun `로그아웃은 access token을 제거하기 전에 FID 해제를 시도한다`() =
        runTest {
            val calls = mutableListOf<String>()
            val pushRepository =
                FakePushRegistrationRepository(
                    onUnregister = { calls += "unregister" },
                )
            val authRepository = FakeAuthRepository { calls += "logout" }
            val logout =
                LogoutUseCase(
                    repository = authRepository,
                    unregisterCurrentPushInstallation =
                        UnregisterCurrentPushInstallationUseCase(
                            installationIdProvider = { "current-fid" },
                            repository = pushRepository,
                        ),
                )

            logout()

            assertEquals(listOf("unregister", "logout"), calls)
        }

    @Test
    fun `FID 해제 실패는 사용자 로그아웃을 막지 않는다`() =
        runTest {
            val authRepository = FakeAuthRepository()
            val pushRepository =
                FakePushRegistrationRepository(
                    onUnregister = { error("offline") },
                )
            val logout =
                LogoutUseCase(
                    repository = authRepository,
                    unregisterCurrentPushInstallation =
                        UnregisterCurrentPushInstallationUseCase(
                            installationIdProvider = { "current-fid" },
                            repository = pushRepository,
                        ),
                )

            logout()

            assertEquals(1, authRepository.logoutCount)
        }

    @Test
    fun `FID 해제가 끝나지 않아도 3초 뒤 사용자 로그아웃을 계속한다`() =
        runTest {
            val authRepository = FakeAuthRepository()
            val pushRepository =
                FakePushRegistrationRepository(
                    onUnregister = { awaitCancellation() },
                )
            val logout =
                LogoutUseCase(
                    repository = authRepository,
                    unregisterCurrentPushInstallation =
                        UnregisterCurrentPushInstallationUseCase(
                            installationIdProvider = { "current-fid" },
                            repository = pushRepository,
                        ),
                )

            logout()

            assertEquals(3_000L, testScheduler.currentTime)
            assertEquals(1, authRepository.logoutCount)
        }

    private class FakePushRegistrationRepository(
        private val onUnregister: suspend () -> Unit = {},
    ) : PushRegistrationRepository {
        val registered = mutableListOf<String>()
        val unregistered = mutableListOf<String>()

        override suspend fun register(firebaseInstallationId: String) {
            registered += firebaseInstallationId
        }

        override suspend fun unregister(firebaseInstallationId: String) {
            onUnregister()
            unregistered += firebaseInstallationId
        }
    }

    private class FakeAuthRepository(
        private val onLogout: () -> Unit = {},
    ) : AuthRepository {
        var logoutCount = 0

        override fun observeSessionState(): Flow<com.soma369.laimory.core.domain.model.auth.AuthSessionState> = emptyFlow()

        override fun observeSignedInAccount(): Flow<com.soma369.laimory.core.domain.model.auth.SignedInAccount?> = emptyFlow()

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) = Unit

        override suspend fun logout() {
            logoutCount++
            onLogout()
        }
    }
}
