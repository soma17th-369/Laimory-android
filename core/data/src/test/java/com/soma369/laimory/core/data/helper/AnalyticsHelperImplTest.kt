package com.soma369.laimory.core.data.helper

import com.soma369.laimory.core.data.analytics.AnalyticsBucket
import com.soma369.laimory.core.data.analytics.AnalyticsDedupeStore
import com.soma369.laimory.core.data.analytics.AnalyticsPayload
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKey
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionState
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPromptContext
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

    private fun helper(
        buckets: Set<AnalyticsBucket>,
        dedupeStore: AnalyticsDedupeStore = InMemoryDedupeStore(),
    ) = AnalyticsHelperImpl(buckets = buckets, dedupeStore = dedupeStore)

    private class RecordingBucket(
        private val failing: Boolean = false,
        override val isEnabled: Boolean = true,
    ) : AnalyticsBucket {
        val sent = mutableListOf<AnalyticsPayload>()

        override suspend fun send(payload: AnalyticsPayload) {
            if (failing) throw IllegalStateException("bucket down")
            sent += payload
        }

        override fun setEnabled(enabled: Boolean) = Unit
    }

    private class InMemoryDedupeStore : AnalyticsDedupeStore {
        private val marked = mutableSetOf<String>()

        override suspend fun markIfFirst(key: String): Boolean = marked.add(key)
    }

    private object FailingDedupeStore : AnalyticsDedupeStore {
        override suspend fun markIfFirst(key: String): Boolean = throw IllegalStateException("store down")
    }
}
