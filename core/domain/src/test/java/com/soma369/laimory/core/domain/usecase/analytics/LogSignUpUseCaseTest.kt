package com.soma369.laimory.core.domain.usecase.analytics

import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKey
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKeys
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LogSignUpUseCaseTest {
    private val analyticsHelper = RecordingAnalyticsHelper()

    @Test
    fun `서버가 온보딩 전이라고 답하면 가입으로 한 번 남긴다`() =
        runTest {
            LogSignUpUseCase(FakeOnboardingRepository(Result.success(false)), analyticsHelper)(SocialLoginProvider.GOOGLE)

            assertEquals(
                listOf(AnalyticsDedupeKeys.SIGN_UP to AnalyticsEvent.SignUp(SocialLoginProvider.GOOGLE)),
                analyticsHelper.loggedOnce,
            )
        }

    @Test
    fun `온보딩을 마친 계정은 가입이 아니다`() =
        runTest {
            LogSignUpUseCase(FakeOnboardingRepository(Result.success(true)), analyticsHelper)(SocialLoginProvider.GOOGLE)

            assertTrue(analyticsHelper.loggedOnce.isEmpty())
        }

    @Test
    fun `조회가 실패하면 추정하지 않고 남기지 않는다`() =
        runTest {
            LogSignUpUseCase(FakeOnboardingRepository(Result.failure(IOException())), analyticsHelper)(SocialLoginProvider.KAKAO)

            assertTrue(analyticsHelper.loggedOnce.isEmpty())
        }

    @Test
    fun `이 기기에서 끝내고 못 올린 온보딩이면 서버가 모르더라도 가입이 아니다`() =
        runTest {
            LogSignUpUseCase(FakeOnboardingRepository(Result.success(false), pending = true), analyticsHelper)(SocialLoginProvider.KAKAO)

            assertTrue(analyticsHelper.loggedOnce.isEmpty())
        }

    private class FakeOnboardingRepository(
        private val remoteCompletion: Result<Boolean>,
        private val pending: Boolean = false,
    ) : OnboardingRepository {
        override suspend fun cachedCompletion(): Boolean? = null

        override suspend fun cacheCompletion(isCompleted: Boolean) = Unit

        override suspend fun isAgeConfirmed(): Boolean = false

        override suspend fun cacheCompletionWithAgeConfirmation() = Unit

        override suspend fun recordCompletion() = Unit

        override suspend fun isCompletionPending(): Boolean = pending

        override suspend fun setCompletionPending(isPending: Boolean) = Unit

        override suspend fun fetchCompletion(): Result<Boolean> = remoteCompletion

        override fun observeLastPageKey(): Flow<String?> = emptyFlow()

        override suspend fun saveProgress(pageKey: String) = Unit

        override suspend fun flowId(): String = "ob_test"

        override suspend fun clear() = Unit
    }

    private class RecordingAnalyticsHelper : AnalyticsHelper {
        val loggedOnce = mutableListOf<Pair<AnalyticsDedupeKey, AnalyticsEvent>>()

        override suspend fun log(event: AnalyticsEvent) = Unit

        override suspend fun logOnce(
            key: AnalyticsDedupeKey,
            event: AnalyticsEvent,
        ) {
            loggedOnce += key to event
        }

        override suspend fun forgetOnce(key: AnalyticsDedupeKey) = Unit

        override fun setUserId(userId: Long?) = Unit
    }
}
