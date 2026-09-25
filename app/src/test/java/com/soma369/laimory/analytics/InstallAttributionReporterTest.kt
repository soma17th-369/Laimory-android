package com.soma369.laimory.analytics

import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKey
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.InstallAttribution
import com.soma369.laimory.core.domain.model.analytics.InstallAttributionRecord
import com.soma369.laimory.core.domain.model.analytics.InstallCampaign
import com.soma369.laimory.core.domain.model.analytics.InstallReferrerStatus
import com.soma369.laimory.core.domain.repository.InstallAttributionRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallAttributionReporterTest {
    private val metaReferrer = "utm_source=meta&utm_medium=paid&utm_campaign=sleep_hook_20260924"

    @Test
    fun `조회해 확정한 결과를 남기고 속성과 확정 이벤트를 보낸다`() =
        runTest {
            val source = FakeSource(InstallReferrerLookup.Found(metaReferrer, clickAtSeconds = 100, installBeginAtSeconds = 200))
            val repository = FakeRepository()
            val helper = RecordingHelper()

            reporter(source, repository, helper).report()

            val saved = repository.saved!!
            assertEquals(InstallReferrerStatus.PARSED, saved.status)
            assertEquals(100_000L, saved.clickAtMillis)
            assertEquals(200_000L, saved.installBeginAtMillis)
            assertEquals(listOf(saved), helper.attributions)
            val (key, event) = helper.loggedOnce.single()
            assertEquals("install_attribution_resolved:install-1", key.value)
            assertEquals(AnalyticsEvent.InstallAttributionResolved(saved), event)
        }

    @Test
    fun `Play 가 시각을 주지 않으면 비워 둔다`() =
        runTest {
            val repository = FakeRepository()

            reporter(FakeSource(InstallReferrerLookup.Found(null, 0, 0)), repository, RecordingHelper()).report()

            assertEquals(InstallAttribution(InstallReferrerStatus.NO_CAMPAIGN), repository.saved)
        }

    @Test
    fun `확정된 결과가 있으면 다시 조회하지 않고 속성만 다시 건다`() =
        runTest {
            val stored = InstallAttribution(InstallReferrerStatus.PROVIDER, InstallCampaign(source = "google-play"))
            val source = FakeSource(InstallReferrerLookup.Transient)
            val helper = RecordingHelper()

            reporter(source, FakeRepository(stored = stored), helper).report()

            assertEquals(0, source.calls)
            assertEquals(listOf(stored), helper.attributions)
            assertEquals(1, helper.loggedOnce.size)
        }

    @Test
    fun `미지원이면 바로 unavailable 로 확정한다`() =
        runTest {
            val source = FakeSource(InstallReferrerLookup.Unsupported)
            val repository = FakeRepository()

            reporter(source, repository, RecordingHelper()).report()

            assertEquals(1, source.calls)
            assertEquals(InstallReferrerStatus.UNAVAILABLE, repository.saved?.status)
        }

    @Test
    fun `일시 오류는 실행마다 정해진 횟수만 다시 묻고 이번 실행은 확정하지 않는다`() =
        runTest {
            val source = FakeSource(InstallReferrerLookup.Transient)
            val repository = FakeRepository()
            val helper = RecordingHelper()

            reporter(source, repository, helper).report()

            assertEquals(InstallAttributionReporter.MAX_ATTEMPTS, source.calls)
            assertEquals(1, repository.failedLaunchCount)
            assertNull(repository.saved)
            assertTrue(helper.attributions.isEmpty())
            assertTrue(helper.loggedOnce.isEmpty())
        }

    @Test
    fun `일시 오류 뒤 다시 물어 성공하면 확정한다`() =
        runTest {
            val source =
                FakeSource(
                    InstallReferrerLookup.Transient,
                    InstallReferrerLookup.Found(metaReferrer, 0, 0),
                )
            val repository = FakeRepository()

            reporter(source, repository, RecordingHelper()).report()

            assertEquals(2, source.calls)
            assertEquals(InstallReferrerStatus.PARSED, repository.saved?.status)
        }

    @Test
    fun `일시 오류가 정해진 실행 수까지 이어지면 unavailable 로 확정한다`() =
        runTest {
            val repository = FakeRepository(failedLaunchCount = InstallAttributionReporter.MAX_FAILED_LAUNCHES - 1)
            val helper = RecordingHelper()

            reporter(FakeSource(InstallReferrerLookup.Transient), repository, helper).report()

            assertEquals(InstallReferrerStatus.UNAVAILABLE, repository.saved?.status)
            assertEquals(1, helper.loggedOnce.size)
        }

    @Test
    fun `저장소가 실패해도 예외를 밖으로 내지 않는다`() =
        runTest {
            val helper = RecordingHelper()

            reporter(FakeSource(InstallReferrerLookup.Unsupported), FakeRepository(failing = true), helper).report()

            assertTrue(helper.attributions.isEmpty())
        }

    @Test
    fun `조회 결과를 문자열로 찍어도 원문이 나오지 않는다`() {
        val lookup = InstallReferrerLookup.Found(metaReferrer, 0, 0)

        assertFalse(lookup.toString().contains("utm_"))
    }

    private fun TestScope.reporter(
        source: InstallReferrerSource,
        repository: InstallAttributionRepository,
        helper: AnalyticsHelper,
    ) = InstallAttributionReporter(
        source = source,
        repository = repository,
        analyticsHelper = helper,
        applicationScope = this,
    )

    private class FakeSource(
        vararg lookups: InstallReferrerLookup,
    ) : InstallReferrerSource {
        private val queue = lookups.toList()
        var calls = 0

        override suspend fun fetch(): InstallReferrerLookup = queue[minOf(calls++, queue.lastIndex)]
    }

    private class FakeRepository(
        private val stored: InstallAttribution? = null,
        var failedLaunchCount: Int = 0,
        private val failing: Boolean = false,
    ) : InstallAttributionRepository {
        var saved: InstallAttribution? = null

        override suspend fun load(): InstallAttributionRecord {
            if (failing) throw IllegalStateException("store down")
            return InstallAttributionRecord(installId = "install-1", attribution = stored, failedLaunchCount = failedLaunchCount)
        }

        override suspend fun saveResolved(attribution: InstallAttribution) {
            saved = attribution
        }

        override suspend fun countFailedLaunch(): Int = ++failedLaunchCount
    }

    private class RecordingHelper : AnalyticsHelper {
        val loggedOnce = mutableListOf<Pair<AnalyticsDedupeKey, AnalyticsEvent>>()
        val attributions = mutableListOf<InstallAttribution>()

        override suspend fun log(event: AnalyticsEvent) = Unit

        override suspend fun logOnce(
            key: AnalyticsDedupeKey,
            event: AnalyticsEvent,
        ) {
            loggedOnce += key to event
        }

        override suspend fun forgetOnce(key: AnalyticsDedupeKey) = Unit

        override fun setUserId(userId: Long?) = Unit

        override fun setInstallAttribution(attribution: InstallAttribution) {
            attributions += attribution
        }
    }
}
