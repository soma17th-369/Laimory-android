package com.soma369.laimory.core.data.analytics

import com.soma369.laimory.core.domain.model.analytics.AnalyticsCompletionOutcome
import com.soma369.laimory.core.domain.model.analytics.AnalyticsCreateResult
import com.soma369.laimory.core.domain.model.analytics.AnalyticsCreateStopReason
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsFailureCode
import com.soma369.laimory.core.domain.model.analytics.AnalyticsItemCounts
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionState
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPromptContext
import com.soma369.laimory.core.domain.model.analytics.AnalyticsReadyTrigger
import com.soma369.laimory.core.domain.model.analytics.AnalyticsRecordDayRelation
import com.soma369.laimory.core.domain.model.analytics.AnalyticsSourceGroup
import com.soma369.laimory.core.domain.model.analytics.AnalyticsTimelineState
import com.soma369.laimory.core.domain.model.collection.ItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 전송 이름과 값을 고정한다.
 *
 * 대시보드 정의가 이 문자열에 묶여 있어, 코드에서 상수 이름을 바꾸거나 새 값을 더할 때 전송 값이
 * 조용히 따라 바뀌면 지난 데이터와 끊긴다. 값을 바꾸려면 이 테스트를 함께 고쳐야 하고, 그때
 * 지난 데이터와의 단절을 의식하게 된다.
 *
 * enum 값 테스트는 먼저 `entries` 와 기대 맵의 키를 대조한다 — 값을 추가하고 매퍼만 채우면 여기서 걸린다.
 */
class AnalyticsEventMapperTest {
    private val today = AnalyticsRecordDayRelation.TODAY

    @Test
    fun `이벤트마다 이름과 속성 이름을 고정한다`() {
        val expected =
            listOf(
                Expectation(
                    AnalyticsEvent.PermissionRequestStarted(AnalyticsPermissionType.PHOTO, AnalyticsPromptContext.HOME),
                    "permission_request_started",
                    setOf("permission_type", "prompt_context"),
                ),
                Expectation(
                    permissionResult(),
                    "permission_result",
                    setOf("permission_type", "permission_state", "prompt_context"),
                ),
                Expectation(
                    AnalyticsEvent.DataCollectionReady(AnalyticsReadyTrigger.STORED_ITEM),
                    "data_collection_ready",
                    setOf("ready_trigger"),
                ),
                Expectation(AnalyticsEvent.TimelineCreateStarted(today), "timeline_create_started", setOf("record_day_relation")),
                Expectation(
                    AnalyticsEvent.TimelineCreateStopped(AnalyticsCreateStopReason.NO_DATA, today),
                    "timeline_create_stopped",
                    setOf("reason", "record_day_relation"),
                ),
                Expectation(
                    AnalyticsEvent.TimelineEventReviewStarted(today, initialItemCount = 5),
                    "timeline_event_review_started",
                    setOf("record_day_relation"),
                    setOf("initial_event_item_count"),
                ),
                Expectation(
                    reviewCompleted(),
                    "timeline_event_review_completed",
                    setOf("record_day_relation"),
                    setOf("initial_event_item_count", "final_event_item_count", "net_removed_item_count") +
                        listOf("photo", "calendar", "location", "health", "notification").flatMap { group ->
                            listOf("initial_${group}_item_count", "final_${group}_item_count")
                        },
                ),
                Expectation(
                    AnalyticsEvent.TimelineCreateRequested(today, itemCount = 4),
                    "timeline_create_requested",
                    setOf("record_day_relation"),
                    setOf("event_item_count"),
                ),
                Expectation(
                    AnalyticsEvent.TimelineCreateRequestFailed(today, AnalyticsFailureCode.NETWORK),
                    "timeline_create_request_failed",
                    setOf("record_day_relation", "failure_code"),
                ),
                Expectation(
                    AnalyticsEvent.TimelineCreateResult(AnalyticsCreateResult.FAILURE, AnalyticsFailureCode.SERVER),
                    "timeline_create_result",
                    setOf("result", "failure_code"),
                ),
                Expectation(
                    AnalyticsEvent.TimelineOpened(AnalyticsTimelineState.DRAFT, today),
                    "timeline_opened",
                    setOf("timeline_state", "record_day_relation"),
                ),
                Expectation(AnalyticsEvent.TimelineCompletionStarted(today), "timeline_completion_started", setOf("record_day_relation")),
                Expectation(
                    AnalyticsEvent.TimelineCompleted(today, AnalyticsCompletionOutcome.TRANSITIONED),
                    "timeline_completed",
                    setOf("record_day_relation", "completion_outcome"),
                ),
                Expectation(
                    AnalyticsEvent.TimelineCompletionFailed(today, AnalyticsFailureCode.UNKNOWN),
                    "timeline_completion_failed",
                    setOf("record_day_relation", "failure_code"),
                ),
            )

        expected.forEach { expectation ->
            val payload = expectation.event.toPayload()
            assertEquals(expectation.name, payload.name)
            assertEquals(expectation.name, expectation.strings, payload.strings.keys)
            assertEquals(expectation.name, expectation.counts + "schema_version", payload.counts.keys)
            assertEquals(1L, payload.counts["schema_version"])
        }
    }

    @Test
    fun `성공한 생성 결과에는 실패 코드를 싣지 않는다`() {
        val payload = AnalyticsEvent.TimelineCreateResult(AnalyticsCreateResult.SUCCESS, failureCode = null).toPayload()

        assertEquals("success", payload.strings["result"])
        assertFalse(payload.strings.containsKey("failure_code"))
    }

    @Test
    fun `만들기 확정에 합계·뺀 수·묶음별 건수를 싣는다`() {
        val payload = reviewCompleted().toPayload()

        assertEquals(7L, payload.counts["initial_event_item_count"])
        assertEquals(5L, payload.counts["final_event_item_count"])
        assertEquals(2L, payload.counts["net_removed_item_count"])
        // 머문 곳과 이동은 위치 하나로 센다.
        assertEquals(3L, payload.counts["initial_location_item_count"])
        assertEquals(2L, payload.counts["final_calendar_item_count"])
        // 없는 묶음도 0 으로 싣는다.
        assertEquals(0L, payload.counts["initial_health_item_count"])
    }

    @Test
    fun `묶음 전송값을 고정한다`() {
        val expected =
            mapOf(
                AnalyticsSourceGroup.PHOTO to "photo",
                AnalyticsSourceGroup.CALENDAR to "calendar",
                AnalyticsSourceGroup.LOCATION to "location",
                AnalyticsSourceGroup.HEALTH to "health",
                AnalyticsSourceGroup.NOTIFICATION to "notification",
            )

        assertEquals(AnalyticsSourceGroup.entries.toSet(), expected.keys)
        val counts = reviewCompleted().toPayload().counts
        expected.values.forEach { wireValue ->
            assertTrue(counts.containsKey("initial_${wireValue}_item_count"))
            assertTrue(counts.containsKey("final_${wireValue}_item_count"))
        }
    }

    @Test
    fun `권한 종류 전송값을 고정한다`() =
        assertWireValues(
            mapOf(
                AnalyticsPermissionType.PHOTO to "photo",
                AnalyticsPermissionType.CALENDAR to "calendar",
                AnalyticsPermissionType.LOCATION_FOREGROUND to "location_foreground",
                AnalyticsPermissionType.LOCATION_BACKGROUND to "location_background",
                AnalyticsPermissionType.HEALTH_CONNECT to "health_connect",
                AnalyticsPermissionType.PUSH_NOTIFICATION to "push_notification",
                AnalyticsPermissionType.NOTIFICATION_LISTENER to "notification_listener",
                AnalyticsPermissionType.ACTIVITY_RECOGNITION to "activity_recognition",
            ),
            AnalyticsPermissionType.entries,
            "permission_type",
        ) { permissionResult(permission = it) }

    @Test
    fun `권한 상태 전송값을 고정한다`() =
        assertWireValues(
            mapOf(
                AnalyticsPermissionState.GRANTED to "granted",
                AnalyticsPermissionState.PARTIAL to "partial",
                AnalyticsPermissionState.DENIED to "denied",
                AnalyticsPermissionState.SETTINGS_REQUIRED to "settings_required",
                AnalyticsPermissionState.UNAVAILABLE to "unavailable",
            ),
            AnalyticsPermissionState.entries,
            "permission_state",
        ) { permissionResult(state = it) }

    @Test
    fun `권한을 요청한 자리 전송값을 고정한다`() =
        assertWireValues(
            mapOf(
                AnalyticsPromptContext.APP_START to "app_start",
                AnalyticsPromptContext.HOME to "home",
                AnalyticsPromptContext.COLLECTION_LAB to "collection_lab",
                AnalyticsPromptContext.SETTINGS to "settings",
            ),
            AnalyticsPromptContext.entries,
            "prompt_context",
        ) { permissionResult(promptContext = it) }

    @Test
    fun `준비 계기 전송값을 고정한다`() =
        assertWireValues(
            mapOf(
                AnalyticsReadyTrigger.STORED_ITEM to "stored_item",
                AnalyticsReadyTrigger.PHOTO_PERMISSION to "photo_permission",
            ),
            AnalyticsReadyTrigger.entries,
            "ready_trigger",
        ) { AnalyticsEvent.DataCollectionReady(it) }

    @Test
    fun `오늘과의 관계 전송값을 고정한다`() =
        assertWireValues(
            mapOf(
                AnalyticsRecordDayRelation.TODAY to "today",
                AnalyticsRecordDayRelation.YESTERDAY to "yesterday",
                AnalyticsRecordDayRelation.OLDER to "older",
                AnalyticsRecordDayRelation.FUTURE to "future",
            ),
            AnalyticsRecordDayRelation.entries,
            "record_day_relation",
        ) { AnalyticsEvent.TimelineCreateStarted(it) }

    @Test
    fun `중단 이유 전송값을 고정한다`() =
        assertWireValues(
            mapOf(
                AnalyticsCreateStopReason.NO_DATA to "no_data",
                AnalyticsCreateStopReason.ALL_EXCLUDED to "all_excluded",
                AnalyticsCreateStopReason.CANCELLED to "cancelled",
            ),
            AnalyticsCreateStopReason.entries,
            "reason",
        ) { AnalyticsEvent.TimelineCreateStopped(it, today) }

    @Test
    fun `실패 코드 전송값을 고정한다`() =
        assertWireValues(
            mapOf(
                AnalyticsFailureCode.NETWORK to "network",
                AnalyticsFailureCode.TIMEOUT to "timeout",
                AnalyticsFailureCode.AUTH to "auth",
                AnalyticsFailureCode.INSUFFICIENT_EVENT to "insufficient_event",
                AnalyticsFailureCode.INVALID_INPUT to "invalid_input",
                AnalyticsFailureCode.SERVER to "server",
                AnalyticsFailureCode.RESULT_UNAVAILABLE to "result_unavailable",
                AnalyticsFailureCode.UNKNOWN to "unknown",
            ),
            AnalyticsFailureCode.entries,
            "failure_code",
        ) { AnalyticsEvent.TimelineCompletionFailed(today, it) }

    @Test
    fun `생성 결과 전송값을 고정한다`() =
        assertWireValues(
            mapOf(
                AnalyticsCreateResult.SUCCESS to "success",
                AnalyticsCreateResult.FAILURE to "failure",
            ),
            AnalyticsCreateResult.entries,
            "result",
        ) { AnalyticsEvent.TimelineCreateResult(it, failureCode = null) }

    @Test
    fun `타임라인 상태 전송값을 고정한다`() =
        assertWireValues(
            mapOf(
                AnalyticsTimelineState.DRAFT to "draft",
                AnalyticsTimelineState.SAVED to "saved",
            ),
            AnalyticsTimelineState.entries,
            "timeline_state",
        ) { AnalyticsEvent.TimelineOpened(it, today) }

    @Test
    fun `완료 방식 전송값을 고정한다`() =
        assertWireValues(
            mapOf(
                AnalyticsCompletionOutcome.TRANSITIONED to "transitioned",
                AnalyticsCompletionOutcome.RECOVERED to "recovered",
            ),
            AnalyticsCompletionOutcome.entries,
            "completion_outcome",
        ) { AnalyticsEvent.TimelineCompleted(today, it) }

    private fun <T> assertWireValues(
        expected: Map<T, String>,
        entries: List<T>,
        param: String,
        eventOf: (T) -> AnalyticsEvent,
    ) {
        assertEquals(entries.toSet(), expected.keys)
        expected.forEach { (value, wireValue) ->
            assertEquals(wireValue, eventOf(value).toPayload().strings[param])
        }
    }

    /** 최초 7(사진 1 · 일정 3 · 위치 3) → 최종 5(사진 1 · 일정 2 · 위치 2). */
    private fun reviewCompleted() =
        AnalyticsEvent.TimelineEventReviewCompleted(
            recordDayRelation = today,
            initialCounts =
                AnalyticsItemCounts.of(
                    listOf(ItemType.PHOTO) + List(3) { ItemType.CALENDAR } + listOf(ItemType.STAY, ItemType.STAY, ItemType.MOVEMENT),
                ),
            finalCounts =
                AnalyticsItemCounts.of(
                    listOf(ItemType.PHOTO) + List(2) { ItemType.CALENDAR } + listOf(ItemType.STAY, ItemType.MOVEMENT),
                ),
        )

    private fun permissionResult(
        permission: AnalyticsPermissionType = AnalyticsPermissionType.PHOTO,
        state: AnalyticsPermissionState = AnalyticsPermissionState.GRANTED,
        promptContext: AnalyticsPromptContext = AnalyticsPromptContext.HOME,
    ) = AnalyticsEvent.PermissionResult(permission = permission, state = state, promptContext = promptContext)

    private data class Expectation(
        val event: AnalyticsEvent,
        val name: String,
        val strings: Set<String>,
        val counts: Set<String> = emptySet(),
    )
}
