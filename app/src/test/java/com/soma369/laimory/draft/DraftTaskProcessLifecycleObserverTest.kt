package com.soma369.laimory.draft

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DraftTaskProcessLifecycleObserverTest {
    @Test
    fun `지연된 background 전환은 다음 start에서 취소한다`() =
        runTest {
            val coordinator = DelayedBackgroundCoordinator()
            val authRepository = FakeAuthRepository(AuthSessionState.Authenticated)
            val observer =
                DraftTaskProcessLifecycleObserver(
                    coordinator = coordinator,
                    observeAuthSessionUseCase = ObserveAuthSessionUseCase(authRepository),
                    applicationScope = backgroundScope,
                )
            val owner = TestLifecycleOwner()

            observer.onStop(owner)
            runCurrent()
            observer.onStart(owner)
            coordinator.allowBackground.complete(Unit)
            runCurrent()

            assertEquals(0, coordinator.backgroundCount)
            assertEquals(1, coordinator.foregroundCount)
        }

    private class DelayedBackgroundCoordinator : DraftTaskCoordinator {
        override val state: StateFlow<DraftTaskTrackingState> =
            MutableStateFlow(DraftTaskTrackingState.Idle)
        val allowBackground = CompletableDeferred<Unit>()
        var foregroundCount = 0
        var backgroundCount = 0

        override suspend fun start(
            taskId: String,
            recordDate: LocalDate,
        ) = Unit

        override suspend fun onForeground() {
            foregroundCount++
        }

        override suspend fun onBackground() {
            allowBackground.await()
            backgroundCount++
        }

        override fun retry() = Unit

        override fun continueWaiting() = Unit

        override suspend fun discard() = Unit
    }

    private class FakeAuthRepository(
        initialState: AuthSessionState,
    ) : AuthRepository {
        private val sessionState = MutableStateFlow(initialState)

        override fun observeSessionState(): Flow<AuthSessionState> = sessionState

        override fun observeSignedInAccount(): Flow<SignedInAccount?> = flowOf(null)

        override suspend fun issueTokens(
            appCode: String,
            appVerifier: String,
        ) = Unit

        override suspend fun logout() = Unit
    }

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle = registry
    }
}
