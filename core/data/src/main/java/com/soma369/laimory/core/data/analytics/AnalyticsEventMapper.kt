package com.soma369.laimory.core.data.analytics

import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionState
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPromptContext

/** 모든 이벤트에 붙는 스키마 판. 속성 의미가 바뀌면 올려 옛 데이터와 섞이지 않게 한다. */
internal const val ANALYTICS_SCHEMA_VERSION = 1L

private const val PARAM_SCHEMA_VERSION = "schema_version"
private const val PARAM_PERMISSION_TYPE = "permission_type"
private const val PARAM_PERMISSION_STATE = "permission_state"
private const val PARAM_PROMPT_CONTEXT = "prompt_context"

/**
 * 도메인 이벤트를 전송 형태로 바꾼다. **버킷 밖에서 한 번만** 한다 — 버킷을 바꿔도 이름과 속성은
 * 그대로여야 대시보드의 계약이 유지된다.
 *
 * `when` 에 `else` 를 두지 않는다. 이벤트를 추가하면 여기서 컴파일이 깨져 이름을 정하게 만든다.
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

/**
 * 전송 값을 상수마다 적어 둔다.
 *
 * 이름을 규칙으로 변환(`name.lowercase()`)하면 상수 이름과 전송 값이 묶인다. 상수 이름은 코드 사정으로
 * 바뀔 수 있는데, 전송 값은 대시보드 정의와 묶여 있어 함께 바뀌면 컴파일은 통과한 채 지난 데이터와
 * 끊긴다. 여기 적힌 문자열이 계약이고, 상수 이름은 그것과 무관하게 바꿀 수 있다.
 *
 * `when` 에 `else` 를 두지 않아 값을 추가하면 여기서 컴파일이 깨진다. 전체 값은
 * `AnalyticsEventMapperTest` 가 고정한다.
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
