package com.soma369.laimory.core.data.analytics

import com.soma369.laimory.core.domain.model.analytics.AnalyticsConsent
import com.soma369.laimory.core.domain.repository.AnalyticsConsentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsConsentApplierTest {
    private val bucket = SwitchRecordingBucket()
    private val repository = FakeConsentRepository()

    @Test
    fun `미결정이면 빌드 기본값을 건드리지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            start(this)

            assertEquals(emptyList<Boolean>(), bucket.switches)
        }

    @Test
    fun `동의하면 켜고 거부하면 끈다`() =
        runTest(UnconfinedTestDispatcher()) {
            start(this)

            repository.state.value = AnalyticsConsent.GRANTED
            repository.state.value = AnalyticsConsent.DENIED

            assertEquals(listOf(true, false), bucket.switches)
        }

    @Test
    fun `앱이 뜰 때 저장된 동의를 곧바로 적용한다`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.state.value = AnalyticsConsent.GRANTED

            start(this)

            assertEquals(listOf(true), bucket.switches)
        }

    private fun start(scope: TestScope) {
        AnalyticsConsentApplier(
            buckets = setOf(bucket),
            consentRepository = repository,
            applicationScope = scope.backgroundScope,
        ).start()
    }

    private class SwitchRecordingBucket : AnalyticsBucket {
        val switches = mutableListOf<Boolean>()
        override val isEnabled: Boolean get() = switches.lastOrNull() ?: false

        override suspend fun send(payload: AnalyticsPayload) = Unit

        override fun setEnabled(enabled: Boolean) {
            switches += enabled
        }
    }

    private class FakeConsentRepository : AnalyticsConsentRepository {
        val state = MutableStateFlow(AnalyticsConsent.UNDECIDED)
        override val consent = state

        override suspend fun set(consent: AnalyticsConsent) {
            state.value = consent
        }
    }
}
