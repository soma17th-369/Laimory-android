package com.soma369.laimory.core.data.helper

import com.soma369.laimory.core.data.analytics.AnalyticsBucket
import com.soma369.laimory.core.data.analytics.AnalyticsDedupeStore
import com.soma369.laimory.core.data.analytics.AnalyticsPayload
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKey
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionState
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPromptContext
import com.soma369.laimory.core.domain.model.analytics.InstallAttribution
import com.soma369.laimory.core.domain.model.analytics.InstallCampaign
import com.soma369.laimory.core.domain.model.analytics.InstallReferrerStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsHelperImplTest {
    private val event =
        AnalyticsEvent.PermissionResult(
            permission = AnalyticsPermissionType.LOCATION_BACKGROUND,
            state = AnalyticsPermissionState.SETTINGS_REQUIRED,
            promptContext = AnalyticsPromptContext.COLLECTION_LAB,
        )

    @Test
    fun `이벤트를 전송 형태로 바꿔 보낸다`() =
        runTest {
            val bucket = RecordingBucket()
            val helper = helper(buckets = setOf(bucket))

            helper.log(event)

            val payload = bucket.sent.single()
            assertEquals("permission_result", payload.name)
            assertEquals("location_background", payload.strings["permission_type"])
            assertEquals("settings_required", payload.strings["permission_state"])
            assertEquals("collection_lab", payload.strings["prompt_context"])
        }

    @Test
    fun `모든 이벤트에 스키마 판을 싣는다`() =
        runTest {
            val bucket = RecordingBucket()

            helper(buckets = setOf(bucket)).log(event)

            assertEquals(1L, bucket.sent.single().counts["schema_version"])
        }

    @Test
    fun `버킷이 여럿이면 모두 보낸다`() =
        runTest {
            val first = RecordingBucket()
            val second = RecordingBucket()

            helper(buckets = setOf(first, second)).log(event)

            assertEquals(1, first.sent.size)
            assertEquals(1, second.sent.size)
        }

    @Test
    fun `한 버킷이 실패해도 나머지는 보내고 앱은 계속된다`() =
        runTest {
            val failing = RecordingBucket(failing = true)
            val healthy = RecordingBucket()

            helper(buckets = setOf(failing, healthy)).log(event)

            assertEquals(1, healthy.sent.size)
        }

    @Test
    fun `같은 키로는 한 번만 보낸다`() =
        runTest {
            val bucket = RecordingBucket()
            val helper = helper(buckets = setOf(bucket))
            val key = AnalyticsDedupeKey("permission_result:user-1")

            helper.logOnce(key, event)
            helper.logOnce(key, event)

            assertEquals(1, bucket.sent.size)
        }

    @Test
    fun `판정 기록은 버킷으로 보내지 않는다`() =
        runTest {
            val bucket = RecordingBucket()
            val key = AnalyticsDedupeKey("permission_result:user-1")

            helper(buckets = setOf(bucket)).logOnce(key, event)

            val payload = bucket.sent.single()
            assertTrue(payload.strings.values.none { value -> value.contains("user-1") })
            assertTrue(payload.strings.keys.none { name -> name.contains("dedupe") })
        }

    @Test
    fun `수집이 꺼져 있으면 판정 키를 소비하지 않는다`() =
        runTest {
            val disabled = RecordingBucket(isEnabled = false)
            val dedupeStore = InMemoryDedupeStore()
            val key = AnalyticsDedupeKey("data_collection_ready:install-1")

            helper(buckets = setOf(disabled), dedupeStore = dedupeStore).logOnce(key, event)

            assertTrue(disabled.sent.isEmpty())
            // 꺼진 동안 소비됐다면, 켠 뒤에도 이 설치에서는 영영 나가지 않는다.
            val enabled = RecordingBucket()
            helper(buckets = setOf(enabled), dedupeStore = dedupeStore).logOnce(key, event)
            assertEquals(1, enabled.sent.size)
        }

    @Test
    fun `판정 기록을 읽지 못하면 보내지 않는다`() =
        runTest {
            val bucket = RecordingBucket()
            val helper = helper(buckets = setOf(bucket), dedupeStore = FailingDedupeStore)

            helper.logOnce(AnalyticsDedupeKey("permission_result:user-1"), event)

            assertTrue(bucket.sent.isEmpty())
        }

    @Test
    fun `판정을 잊으면 같은 키가 다시 나간다`() =
        runTest {
            val bucket = RecordingBucket()
            val key = AnalyticsDedupeKey("timeline_completed:2:2026-09-18")
            val helper = helper(buckets = setOf(bucket), dedupeStore = InMemoryDedupeStore())
            helper.logOnce(key, event)

            helper.forgetOnce(key)
            helper.logOnce(key, event)

            assertEquals(2, bucket.sent.size)
        }

    @Test
    fun `뿌리 키를 잊으면 회원 구분이 붙은 키도 함께 지운다`() =
        runTest {
            // 지우는 시점에는 회원 정보를 아직 못 받았을 수 있다. 아는 회원 것만 지우면 남은 판정이 다음 기록을 막는다.
            val bucket = RecordingBucket()
            val root = AnalyticsDedupeKey("timeline_completed:2026-09-18")
            val perUser = AnalyticsDedupeKey("timeline_completed:2026-09-18:2")
            val helper = helper(buckets = setOf(bucket), dedupeStore = InMemoryDedupeStore())
            helper.logOnce(perUser, event)

            helper.forgetOnce(root)
            helper.logOnce(perUser, event)

            assertEquals(2, bucket.sent.size)
        }

    @Test
    fun `회원 식별자를 모든 버킷의 사용자 구분으로 건다`() {
        val first = RecordingBucket()
        val second = RecordingBucket()

        helper(buckets = setOf(first, second)).setUserId(42L)

        assertEquals(listOf<String?>("42"), first.userIds)
        assertEquals(listOf<String?>("42"), second.userIds)
    }

    @Test
    fun `null 이면 사용자 구분을 푼다`() {
        val bucket = RecordingBucket()

        helper(buckets = setOf(bucket)).setUserId(null)

        assertEquals(listOf<String?>(null), bucket.userIds)
    }

    @Test
    fun `한 버킷이 사용자 구분에 실패해도 나머지는 건다`() {
        val failing = RecordingBucket(failing = true)
        val healthy = RecordingBucket()

        helper(buckets = setOf(failing, healthy)).setUserId(42L)

        assertEquals(listOf<String?>("42"), healthy.userIds)
    }

    @Test
    fun `설치 유입을 사용자 속성으로 건다`() {
        val bucket = RecordingBucket()

        helper(buckets = setOf(bucket)).setInstallAttribution(
            InstallAttribution(
                status = InstallReferrerStatus.PARSED,
                campaign = InstallCampaign(source = "meta", medium = "paid", campaign = "sleep_hook_20260924"),
            ),
        )

        assertEquals(
            mapOf(
                "referrer_status" to "parsed",
                "install_source" to "meta",
                "install_medium" to "paid",
                "install_campaign" to "sleep_hook_20260924",
                "install_content" to null,
                "install_campaign_id" to null,
            ),
            bucket.userProperties,
        )
    }

    @Test
    fun `36자를 넘는 값은 자르지 않고 걸지 않는다`() {
        val bucket = RecordingBucket()
        val longCampaign = "a".repeat(28) + "_20260924"

        helper(buckets = setOf(bucket)).setInstallAttribution(
            InstallAttribution(
                status = InstallReferrerStatus.PARSED,
                campaign = InstallCampaign(source = "meta", medium = "paid", campaign = longCampaign),
            ),
        )

        assertEquals(37, longCampaign.length)
        assertEquals(null, bucket.userProperties["install_campaign"])
        assertEquals("meta", bucket.userProperties["install_source"])
    }

    @Test
    fun `캠페인이 없는 상태면 상태만 건다`() {
        val bucket = RecordingBucket()

        helper(buckets = setOf(bucket)).setInstallAttribution(InstallAttribution(InstallReferrerStatus.NO_CAMPAIGN))

        assertEquals("no_campaign", bucket.userProperties["referrer_status"])
        assertTrue(bucket.userProperties.filterKeys { it.startsWith("install_") }.values.all { it == null })
    }

    @Test
    fun `한 버킷이 속성 설정에 실패해도 나머지는 건다`() {
        val failing = RecordingBucket(failing = true)
        val healthy = RecordingBucket()

        helper(buckets = setOf(failing, healthy)).setInstallAttribution(InstallAttribution(InstallReferrerStatus.INVALID))

        assertEquals("invalid", healthy.userProperties["referrer_status"])
    }

    private fun helper(
        buckets: Set<AnalyticsBucket>,
        dedupeStore: AnalyticsDedupeStore = InMemoryDedupeStore(),
    ) = AnalyticsHelperImpl(buckets = buckets, dedupeStore = dedupeStore)

    private class RecordingBucket(
        private val failing: Boolean = false,
        override val isEnabled: Boolean = true,
    ) : AnalyticsBucket {
        val sent = mutableListOf<AnalyticsPayload>()
        val userIds = mutableListOf<String?>()
        val userProperties = mutableMapOf<String, String?>()

        override suspend fun send(payload: AnalyticsPayload) {
            if (failing) throw IllegalStateException("bucket down")
            sent += payload
        }

        override fun setEnabled(enabled: Boolean) = Unit

        override fun setUserId(userId: String?) {
            if (failing) throw IllegalStateException("bucket down")
            userIds += userId
        }

        override fun setUserProperty(
            name: String,
            value: String?,
        ) {
            if (failing) throw IllegalStateException("bucket down")
            userProperties[name] = value
        }
    }

    private class InMemoryDedupeStore : AnalyticsDedupeStore {
        private val marked = mutableSetOf<String>()

        override suspend fun markIfFirst(key: String): Boolean = marked.add(key)

        override suspend fun forgetFamily(rootKey: String) {
            marked.removeAll { key -> key == rootKey || key.startsWith("$rootKey:") }
        }
    }

    private object FailingDedupeStore : AnalyticsDedupeStore {
        override suspend fun markIfFirst(key: String): Boolean = throw IllegalStateException("store down")

        override suspend fun forgetFamily(rootKey: String) = throw IllegalStateException("store down")
    }
}
