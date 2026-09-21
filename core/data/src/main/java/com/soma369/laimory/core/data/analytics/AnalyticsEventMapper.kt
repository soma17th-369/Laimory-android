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
 * enum 이름을 그대로 쓰지 않고 소문자 snake_case 로 옮긴다.
 *
 * 상수 이름은 코드 사정으로 바뀔 수 있는데, 전송 값은 대시보드 정의와 묶여 있어 조용히 바뀌면
 * 지난 데이터와 끊긴다. 옮기는 규칙을 한 곳에 두어 새 값도 같은 모양이 되게 한다.
 */
private val AnalyticsPermissionType.paramValue: String get() = name.lowercase()
private val AnalyticsPermissionState.paramValue: String get() = name.lowercase()
private val AnalyticsPromptContext.paramValue: String get() = name.lowercase()
