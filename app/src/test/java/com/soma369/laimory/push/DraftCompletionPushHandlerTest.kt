package com.soma369.laimory.push

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.PushRegistrationRepository
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.domain.usecase.push.RegisterPushInstallationUseCase
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DraftCompletionPushHandlerTest {
    private lateinit var previousLogLevel: Logger.Level

    @Before
    fun setUp() {
        previousLogLevel = Logger.minLevel
        Logger.minLevel = Logger.Level.ERROR
    }

    @After
    fun tearDown() {
        Logger.minLevel = previousLogLevel
    }

    @Test
    fun `인증 세션에서 onRegistered FID를 서버에 등록한다`() =
        runTest {
            val pushRepository = FakePushRegistrationRepository()
            val handler =
                handler(
                    authState = AuthSessionState.Authenticated,
                    pushRepository = pushRepository,
                    coordinator = FakeDraftTaskCoordinator(),
                    scope = backgroundScope,
                )

            handler.onRegistered("opaque-fid")
            runCurrent()

            assertEquals(listOf("opaque-fid"), pushRepository.registered)
        }

    @Test
    fun `비인증 세션에서는 onRegistered FID를 서버에 보내지 않는다`() =
        runTest {
            val pushRepository = FakePushRegistrationRepository()
            val handler =
                handler(
                    authState = AuthSessionState.Unauthenticated,
                    pushRepository = pushRepository,
                    coordinator = FakeDraftTaskCoordinator(),
                    scope = backgroundScope,
                )

            handler.onRegistered("opaque-fid")
            runCurrent()

            assertTrue(pushRepository.registered.isEmpty())
        }

    @Test
    fun `유효한 완료 메시지만 coordinator에 전달한다`() =
        runTest {
            val coordinator = FakeDraftTaskCoordinator()
            val handler =
                handler(
                    authState = AuthSessionState.Authenticated,
                    pushRepository = FakePushRegistrationRepository(),
                    coordinator = coordinator,
                    scope = backgroundScope,
                )

            handler.onMessage(mapOf("taskId" to "task-1", "status" to "SUCCESS"))
            handler.onMessage(mapOf("taskId" to "task-2", "status" to "PROCESSING"))
            handler.onMessage(mapOf("status" to "FAILED"))

            assertEquals(listOf("task-1"), coordinator.refreshedTaskIds)
        }

    @Test
    fun `알림 클릭 payload 검증 결과를 반환하고 유효한 작업만 새로고침한다`() =
        runTest {
            val coordinator = FakeDraftTaskCoordinator()
            val handler =
                handler(
                    authState = AuthSessionState.Authenticated,
                    pushRepository = FakePushRegistrationRepository(),
                    coordinator = coordinator,
                    scope = backgroundScope,
                )

            assertFalse(handler.onNotificationOpened("", "SUCCESS"))
            assertTrue(handler.onNotificationOpened("task-1", "FAILED"))

            assertEquals(listOf("task-1"), coordinator.refreshedTaskIds)
        }

    private fun handler(
        authState: AuthSessionState,
        pushRepository: PushRegistrationRepository,
        coordinator: DraftTaskCoordinator,
        scope: kotlinx.coroutines.CoroutineScope,
    ): DraftCompletionPushHandler {
        val authRepository = FakeAuthRepository(authState)
        return DraftCompletionPushHandler(
            registerPushInstallation = RegisterPushInstallationUseCase(pushRepository),
            observeAuthSession = ObserveAuthSessionUseCase(authRepository),
            draftTaskCoordinator = coordinator,
            applicationScope = scope,
        )
    }

    private class FakePushRegistrationRepository : PushRegistrationRepository {
        val registered = mutableListOf<String>()

        override suspend fun register(firebaseInstallationId: String) {
            registered += firebaseInstallationId
        }

        override suspend fun unregister(firebaseInstallationId: String) = Unit
    }

    private class FakeAuthRepository(
        authState: AuthSessionState,
    ) : AuthRepository {
        private val state = MutableStateFlow(authState)

        override fun observeSessionState(): Flow<AuthSessionState> = state

        override fun observeSignedInAccount(): Flow<SignedInAccount?> = MutableStateFlow(null)

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) = Unit

        override suspend fun logout() = Unit
    }

    private class FakeDraftTaskCoordinator : DraftTaskCoordinator {
        override val state: StateFlow<DraftTaskTrackingState> = MutableStateFlow(DraftTaskTrackingState.Idle)
        val refreshedTaskIds = mutableListOf<String>()

        override suspend fun start(
            taskId: String,
            recordDate: LocalDate,
        ) = Unit

        override suspend fun onForeground() = Unit

        override suspend fun onBackground() = Unit

        override fun refreshFromCompletionSignal(taskId: String) {
            refreshedTaskIds += taskId
        }

        override fun retry() = Unit

        override fun continueWaiting() = Unit

        override suspend fun discard() = Unit
    }
}
