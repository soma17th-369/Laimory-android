package com.soma369.laimory.core.data.analytics

import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionState
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPromptContext
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 전송 이름과 값을 고정한다.
 *
 * 대시보드 정의가 이 문자열에 묶여 있어, 코드에서 상수 이름을 바꾸거나 새 값을 더할 때 전송 값이
 * 조용히 따라 바뀌면 지난 데이터와 끊긴다. 값을 바꾸려면 이 테스트를 함께 고쳐야 하고, 그때
 * 지난 데이터와의 단절을 의식하게 된다.
 */
class AnalyticsEventMapperTest {
    @Test
    fun `이벤트 이름을 고정한다`() {
        assertEquals(
            "permission_request_started",
            AnalyticsEvent
                .PermissionRequestStarted(
                    permission = AnalyticsPermissionType.PHOTO,
                    promptContext = AnalyticsPromptContext.HOME,
                ).toPayload()
                .name,
        )
        assertEquals("permission_result", permissionResult(AnalyticsPermissionType.PHOTO).toPayload().name)
    }

    @Test
    fun `권한 종류 전송값을 고정한다`() {
        val expected =
            mapOf(
                AnalyticsPermissionType.PHOTO to "photo",
                AnalyticsPermissionType.CALENDAR to "calendar",
                AnalyticsPermissionType.LOCATION_FOREGROUND to "location_foreground",
                AnalyticsPermissionType.LOCATION_BACKGROUND to "location_background",
                AnalyticsPermissionType.HEALTH_CONNECT to "health_connect",
                AnalyticsPermissionType.PUSH_NOTIFICATION to "push_notification",
                AnalyticsPermissionType.NOTIFICATION_LISTENER to "notification_listener",
                AnalyticsPermissionType.ACTIVITY_RECOGNITION to "activity_recognition",
            )

        // 값을 추가하면 여기서 먼저 실패한다 — 새 값의 전송 이름을 정하게 만든다.
        assertEquals(AnalyticsPermissionType.entries.toSet(), expected.keys)
        expected.forEach { (permission, wireValue) ->
            assertEquals(wireValue, permissionResult(permission).toPayload().strings["permission_type"])
        }
    }

    @Test
    fun `권한 상태 전송값을 고정한다`() {
        val expected =
            mapOf(
                AnalyticsPermissionState.GRANTED to "granted",
                AnalyticsPermissionState.PARTIAL to "partial",
                AnalyticsPermissionState.DENIED to "denied",
                AnalyticsPermissionState.SETTINGS_REQUIRED to "settings_required",
                AnalyticsPermissionState.UNAVAILABLE to "unavailable",
            )

        assertEquals(AnalyticsPermissionState.entries.toSet(), expected.keys)
        expected.forEach { (state, wireValue) ->
            val payload = permissionResult(AnalyticsPermissionType.PHOTO, state = state).toPayload()
            assertEquals(wireValue, payload.strings["permission_state"])
        }
    }

    @Test
    fun `권한을 요청한 자리 전송값을 고정한다`() {
        val expected =
            mapOf(
                AnalyticsPromptContext.APP_START to "app_start",
                AnalyticsPromptContext.HOME to "home",
                AnalyticsPromptContext.COLLECTION_LAB to "collection_lab",
                AnalyticsPromptContext.SETTINGS to "settings",
            )

        assertEquals(AnalyticsPromptContext.entries.toSet(), expected.keys)
        expected.forEach { (promptContext, wireValue) ->
            val payload = permissionResult(AnalyticsPermissionType.PHOTO, promptContext = promptContext).toPayload()
            assertEquals(wireValue, payload.strings["prompt_context"])
        }
    }

    @Test
    fun `속성 이름과 스키마 판을 고정한다`() {
        val payload = permissionResult(AnalyticsPermissionType.PHOTO).toPayload()

        assertEquals(setOf("permission_type", "permission_state", "prompt_context"), payload.strings.keys)
        assertEquals(mapOf("schema_version" to 1L), payload.counts)
    }

    private fun permissionResult(
        permission: AnalyticsPermissionType,
        state: AnalyticsPermissionState = AnalyticsPermissionState.GRANTED,
        promptContext: AnalyticsPromptContext = AnalyticsPromptContext.HOME,
    ) = AnalyticsEvent.PermissionResult(
        permission = permission,
        state = state,
        promptContext = promptContext,
    )
}
