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

/** 모든 이벤트에 붙는 스키마 판. 속성 의미가 바뀌면 올려 옛 데이터와 섞이지 않게 한다. */
internal const val ANALYTICS_SCHEMA_VERSION = 1L

private const val PARAM_SCHEMA_VERSION = "schema_version"
private const val PARAM_PERMISSION_TYPE = "permission_type"
private const val PARAM_PERMISSION_STATE = "permission_state"
private const val PARAM_PROMPT_CONTEXT = "prompt_context"
private const val PARAM_READY_TRIGGER = "ready_trigger"
private const val PARAM_RECORD_DAY_RELATION = "record_day_relation"
private const val PARAM_STOP_REASON = "reason"
private const val PARAM_INITIAL_ITEM_COUNT = "initial_event_item_count"
private const val PARAM_FINAL_ITEM_COUNT = "final_event_item_count"
private const val PARAM_NET_REMOVED_ITEM_COUNT = "net_removed_item_count"
private const val PARAM_ITEM_COUNT = "event_item_count"
private const val PARAM_FAILURE_CODE = "failure_code"
private const val PARAM_RESULT = "result"
private const val PARAM_TIMELINE_STATE = "timeline_state"
private const val PARAM_COMPLETION_OUTCOME = "completion_outcome"
private const val PARAM_AI_EVENT_COUNT = "ai_event_count"
private const val PARAM_AI_MEMO_EVENT_COUNT = "ai_memo_event_count"
private const val PARAM_AI_EDITED_EVENT_COUNT = "ai_edited_event_count"
private const val PARAM_AI_DELETED_EVENT_COUNT = "ai_deleted_event_count"
private const val PARAM_MANUAL_EVENT_COUNT = "manual_event_count"
private const val PARAM_MANUAL_MEMO_EVENT_COUNT = "manual_memo_event_count"

/**
 * 도메인 이벤트를 전송 형태로 바꾼다. **버킷 밖에서 한 번만** 한다 — 버킷을 바꿔도 이름과 속성은
 * 그대로여야 대시보드의 계약이 유지된다.
 *
 * `when` 에 `else` 를 두지 않는다. 이벤트를 추가하면 여기서 컴파일이 깨져 이름을 정하게 만든다.
 * 이름과 값 전체는 `AnalyticsEventMapperTest` 가 고정한다.
 */
internal fun AnalyticsEvent.toPayload(): AnalyticsPayload =
    when (this) {
        is AnalyticsEvent.PermissionRequestStarted ->
            payload(
                name = "permission_request_started",
                strings =
                    mapOf(
                        PARAM_PERMISSION_TYPE to permission.paramValue,
                        PARAM_PROMPT_CONTEXT to promptContext.paramValue,
                    ),
            )
        is AnalyticsEvent.PermissionResult ->
            payload(
                name = "permission_result",
                strings =
                    mapOf(
                        PARAM_PERMISSION_TYPE to permission.paramValue,
                        PARAM_PERMISSION_STATE to state.paramValue,
                        PARAM_PROMPT_CONTEXT to promptContext.paramValue,
                    ),
            )
        is AnalyticsEvent.DataCollectionReady ->
            payload(
                name = "data_collection_ready",
                strings = mapOf(PARAM_READY_TRIGGER to trigger.paramValue),
            )
        is AnalyticsEvent.TimelineCreateStarted ->
            payload(
                name = "timeline_create_started",
                strings = mapOf(PARAM_RECORD_DAY_RELATION to recordDayRelation.paramValue),
            )
        is AnalyticsEvent.TimelineCreateStopped ->
            payload(
                name = "timeline_create_stopped",
                strings =
                    mapOf(
                        PARAM_STOP_REASON to reason.paramValue,
                        PARAM_RECORD_DAY_RELATION to recordDayRelation.paramValue,
                    ),
            )
        is AnalyticsEvent.TimelineEventReviewStarted ->
            payload(
                name = "timeline_event_review_started",
                strings = mapOf(PARAM_RECORD_DAY_RELATION to recordDayRelation.paramValue),
                counts = mapOf(PARAM_INITIAL_ITEM_COUNT to initialItemCount.toLong()),
            )
        is AnalyticsEvent.TimelineEventReviewCompleted ->
            payload(
                name = "timeline_event_review_completed",
                strings = mapOf(PARAM_RECORD_DAY_RELATION to recordDayRelation.paramValue),
                counts =
                    mapOf(
                        PARAM_INITIAL_ITEM_COUNT to initialCounts.total.toLong(),
                        PARAM_FINAL_ITEM_COUNT to finalCounts.total.toLong(),
                        PARAM_NET_REMOVED_ITEM_COUNT to netRemovedItemCount.toLong(),
                    ) + initialCounts.byGroupParams(STAGE_INITIAL) + finalCounts.byGroupParams(STAGE_FINAL),
            )
        is AnalyticsEvent.TimelineCreateRequested ->
            payload(
                name = "timeline_create_requested",
                strings = mapOf(PARAM_RECORD_DAY_RELATION to recordDayRelation.paramValue),
                counts = mapOf(PARAM_ITEM_COUNT to itemCount.toLong()),
            )
        is AnalyticsEvent.TimelineCreateRequestFailed ->
            payload(
                name = "timeline_create_request_failed",
                strings =
                    mapOf(
                        PARAM_RECORD_DAY_RELATION to recordDayRelation.paramValue,
                        PARAM_FAILURE_CODE to failureCode.paramValue,
                    ),
            )
        is AnalyticsEvent.TimelineCreateResult ->
            payload(
                name = "timeline_create_result",
                strings =
                    buildMap {
                        put(PARAM_RESULT, result.paramValue)
                        failureCode?.let { put(PARAM_FAILURE_CODE, it.paramValue) }
                    },
            )
        is AnalyticsEvent.TimelineOpened ->
            payload(
                name = "timeline_opened",
                strings =
                    mapOf(
                        PARAM_TIMELINE_STATE to timelineState.paramValue,
                        PARAM_RECORD_DAY_RELATION to recordDayRelation.paramValue,
                    ),
            )
        is AnalyticsEvent.TimelineCompletionStarted ->
            payload(
                name = "timeline_completion_started",
                strings = mapOf(PARAM_RECORD_DAY_RELATION to recordDayRelation.paramValue),
            )
        is AnalyticsEvent.TimelineCompleted ->
            payload(
                name = "timeline_completed",
                strings =
                    mapOf(
                        PARAM_RECORD_DAY_RELATION to recordDayRelation.paramValue,
                        PARAM_COMPLETION_OUTCOME to completionOutcome.paramValue,
                    ),
                counts =
                    mapOf(
                        PARAM_AI_EVENT_COUNT to eventSummary.aiEventCount.toLong(),
                        PARAM_AI_MEMO_EVENT_COUNT to eventSummary.aiMemoEventCount.toLong(),
                        PARAM_AI_EDITED_EVENT_COUNT to eventSummary.aiEditedEventCount.toLong(),
                        PARAM_AI_DELETED_EVENT_COUNT to eventSummary.aiDeletedEventCount.toLong(),
                        PARAM_MANUAL_EVENT_COUNT to eventSummary.manualEventCount.toLong(),
                        PARAM_MANUAL_MEMO_EVENT_COUNT to eventSummary.manualMemoEventCount.toLong(),
                    ),
            )
        is AnalyticsEvent.TimelineCompletionFailed ->
            payload(
                name = "timeline_completion_failed",
                strings =
                    mapOf(
                        PARAM_RECORD_DAY_RELATION to recordDayRelation.paramValue,
                        PARAM_FAILURE_CODE to failureCode.paramValue,
                    ),
            )
    }

private const val STAGE_INITIAL = "initial"
private const val STAGE_FINAL = "final"

/**
 * 묶음별 건수. 합계 이름(`initial_event_item_count`)의 `event` 자리에 묶음을 넣는다 — `initial_calendar_item_count`.
 * 모든 묶음을 빠짐없이 싣는다(없으면 0).
 */
private fun AnalyticsItemCounts.byGroupParams(stage: String): Map<String, Long> =
    AnalyticsSourceGroup.entries.associate { group ->
        "${stage}_${group.paramValue}_item_count" to countOf(group).toLong()
    }

private fun payload(
    name: String,
    strings: Map<String, String> = emptyMap(),
    counts: Map<String, Long> = emptyMap(),
): AnalyticsPayload =
    AnalyticsPayload(
        name = name,
        strings = strings,
        counts = counts + (PARAM_SCHEMA_VERSION to ANALYTICS_SCHEMA_VERSION),
    )

/*
 * 전송 값을 상수마다 적어 둔다.
 *
 * 이름을 규칙으로 변환(`name.lowercase()`)하면 상수 이름과 전송 값이 묶인다. 상수 이름은 코드 사정으로
 * 바뀔 수 있는데, 전송 값은 대시보드 정의와 묶여 있어 함께 바뀌면 컴파일은 통과한 채 지난 데이터와
 * 끊긴다. 여기 적힌 문자열이 계약이고, 상수 이름은 그것과 무관하게 바꿀 수 있다.
 *
 * `when` 에 `else` 를 두지 않아 값을 추가하면 여기서 컴파일이 깨진다.
 */

private val AnalyticsPermissionType.paramValue: String
    get() =
        when (this) {
            AnalyticsPermissionType.PHOTO -> "photo"
            AnalyticsPermissionType.CALENDAR -> "calendar"
            AnalyticsPermissionType.LOCATION_FOREGROUND -> "location_foreground"
            AnalyticsPermissionType.LOCATION_BACKGROUND -> "location_background"
            AnalyticsPermissionType.HEALTH_CONNECT -> "health_connect"
            AnalyticsPermissionType.PUSH_NOTIFICATION -> "push_notification"
            AnalyticsPermissionType.NOTIFICATION_LISTENER -> "notification_listener"
            AnalyticsPermissionType.ACTIVITY_RECOGNITION -> "activity_recognition"
        }

private val AnalyticsPermissionState.paramValue: String
    get() =
        when (this) {
            AnalyticsPermissionState.GRANTED -> "granted"
            AnalyticsPermissionState.PARTIAL -> "partial"
            AnalyticsPermissionState.DENIED -> "denied"
            AnalyticsPermissionState.SETTINGS_REQUIRED -> "settings_required"
            AnalyticsPermissionState.UNAVAILABLE -> "unavailable"
        }

private val AnalyticsPromptContext.paramValue: String
    get() =
        when (this) {
            AnalyticsPromptContext.APP_START -> "app_start"
            AnalyticsPromptContext.HOME -> "home"
            AnalyticsPromptContext.COLLECTION_LAB -> "collection_lab"
            AnalyticsPromptContext.SETTINGS -> "settings"
        }

private val AnalyticsReadyTrigger.paramValue: String
    get() =
        when (this) {
            AnalyticsReadyTrigger.STORED_ITEM -> "stored_item"
            AnalyticsReadyTrigger.PHOTO_PERMISSION -> "photo_permission"
        }

private val AnalyticsRecordDayRelation.paramValue: String
    get() =
        when (this) {
            AnalyticsRecordDayRelation.TODAY -> "today"
            AnalyticsRecordDayRelation.YESTERDAY -> "yesterday"
            AnalyticsRecordDayRelation.OLDER -> "older"
            AnalyticsRecordDayRelation.FUTURE -> "future"
        }

private val AnalyticsCreateStopReason.paramValue: String
    get() =
        when (this) {
            AnalyticsCreateStopReason.NO_DATA -> "no_data"
            AnalyticsCreateStopReason.ALL_EXCLUDED -> "all_excluded"
            AnalyticsCreateStopReason.CANCELLED -> "cancelled"
        }

private val AnalyticsFailureCode.paramValue: String
    get() =
        when (this) {
            AnalyticsFailureCode.NETWORK -> "network"
            AnalyticsFailureCode.TIMEOUT -> "timeout"
            AnalyticsFailureCode.AUTH -> "auth"
            AnalyticsFailureCode.INSUFFICIENT_EVENT -> "insufficient_event"
            AnalyticsFailureCode.INVALID_INPUT -> "invalid_input"
            AnalyticsFailureCode.SERVER -> "server"
            AnalyticsFailureCode.RESULT_UNAVAILABLE -> "result_unavailable"
            AnalyticsFailureCode.UNKNOWN -> "unknown"
        }

private val AnalyticsCreateResult.paramValue: String
    get() =
        when (this) {
            AnalyticsCreateResult.SUCCESS -> "success"
            AnalyticsCreateResult.FAILURE -> "failure"
        }

private val AnalyticsTimelineState.paramValue: String
    get() =
        when (this) {
            AnalyticsTimelineState.DRAFT -> "draft"
            AnalyticsTimelineState.SAVED -> "saved"
        }

private val AnalyticsSourceGroup.paramValue: String
    get() =
        when (this) {
            AnalyticsSourceGroup.PHOTO -> "photo"
            AnalyticsSourceGroup.CALENDAR -> "calendar"
            AnalyticsSourceGroup.LOCATION -> "location"
            AnalyticsSourceGroup.HEALTH -> "health"
            AnalyticsSourceGroup.NOTIFICATION -> "notification"
        }

private val AnalyticsCompletionOutcome.paramValue: String
    get() =
        when (this) {
            AnalyticsCompletionOutcome.TRANSITIONED -> "transitioned"
            AnalyticsCompletionOutcome.RECOVERED -> "recovered"
        }
