package com.soma369.laimory.core.ui.permission

import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionState
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 권한 KPI 의 근거를 고정한다 — 무엇을 요청으로 세고, 돌아온 상태를 어떤 결과로 보는가.
 */
class DataPermissionAnalyticsTest {
    private val requested = mutableListOf<AnalyticsPermissionType>()

    private fun state(
        granted: Set<DataPermission> = emptySet(),
        locationStep: LocationPermissionStep = LocationPermissionStep.FOREGROUND,
        isPhotoLimited: Boolean = false,
        hasListenerSettings: Boolean = true,
        isHealthAvailable: Boolean = true,
        blocked: Set<DataPermission> = emptySet(),
    ) = DataPermissionState(
        granted = granted,
        locationStep = locationStep,
        isPhotoLimited = isPhotoLimited,
        hasListenerSettings = hasListenerSettings,
        isHealthAvailable = isHealthAvailable,
        blocked = blocked,
        onRequestStarted = { _, type -> requested += type },
        onRequest = {},
    )

    @Test
    fun `위치는 지금 넘으려는 단계를 요청 종류로 본다`() {
        assertEquals(
            AnalyticsPermissionType.LOCATION_FOREGROUND,
            DataPermission.LOCATION.analyticsTypeAt(LocationPermissionStep.FOREGROUND),
        )
        assertEquals(
            AnalyticsPermissionType.LOCATION_BACKGROUND,
            DataPermission.LOCATION.analyticsTypeAt(LocationPermissionStep.BACKGROUND),
        )
        assertEquals(AnalyticsPermissionType.ACTIVITY_RECOGNITION, DataPermission.LOCATION.analyticsTypeAt(LocationPermissionStep.ACTIVITY))
        assertNull(DataPermission.LOCATION.analyticsTypeAt(LocationPermissionStep.GRANTED))
    }

    @Test
    fun `앱 알림 권한은 푸시 알림으로 보낸다`() {
        assertEquals(
            AnalyticsPermissionType.PUSH_NOTIFICATION,
            DataPermission.APP_NOTIFICATION.analyticsTypeAt(LocationPermissionStep.GRANTED),
        )
    }

    @Test
    fun `요청 창을 띄울 때 요청으로 센다`() {
        state().act(DataPermission.CALENDAR)

        assertEquals(listOf(AnalyticsPermissionType.CALENDAR), requested)
    }

    @Test
    fun `이미 허용된 것을 바꾸러 설정에 가는 것은 요청으로 세지 않는다`() {
        state(granted = setOf(DataPermission.CALENDAR)).act(DataPermission.CALENDAR)

        assertTrue(requested.isEmpty())
    }

    @Test
    fun `두 번 거부해 설정으로 보내는 것은 요청으로 센다`() {
        state(blocked = setOf(DataPermission.CALENDAR)).act(DataPermission.CALENDAR)

        assertEquals(listOf(AnalyticsPermissionType.CALENDAR), requested)
    }

    @Test
    fun `전경 위치를 받으면 다음 단계가 남아도 요청한 권한은 허용이다`() {
        val result =
            state(locationStep = LocationPermissionStep.BACKGROUND)
                .analyticsResultOf(DataPermission.LOCATION, AnalyticsPermissionType.LOCATION_FOREGROUND)

        assertEquals(AnalyticsPermissionState.GRANTED, result)
    }

    @Test
    fun `백그라운드를 요청했는데 그대로면 거부, 막혔으면 설정 필요다`() {
        val step = LocationPermissionStep.BACKGROUND

        assertEquals(
            AnalyticsPermissionState.DENIED,
            state(locationStep = step).analyticsResultOf(DataPermission.LOCATION, AnalyticsPermissionType.LOCATION_BACKGROUND),
        )
        assertEquals(
            AnalyticsPermissionState.SETTINGS_REQUIRED,
            state(locationStep = step, blocked = setOf(DataPermission.LOCATION))
                .analyticsResultOf(DataPermission.LOCATION, AnalyticsPermissionType.LOCATION_BACKGROUND),
        )
    }

    @Test
    fun `사진 일부 선택은 부분 허용이다`() {
        val result =
            state(granted = setOf(DataPermission.PHOTO), isPhotoLimited = true)
                .analyticsResultOf(DataPermission.PHOTO, AnalyticsPermissionType.PHOTO)

        assertEquals(AnalyticsPermissionState.PARTIAL, result)
    }

    @Test
    fun `알림 접근 설정이 없는 기기는 쓸 수 없음이다`() {
        val result =
            state(hasListenerSettings = false)
                .analyticsResultOf(DataPermission.NOTIFICATION_LISTENER, AnalyticsPermissionType.NOTIFICATION_LISTENER)

        assertEquals(AnalyticsPermissionState.UNAVAILABLE, result)
    }
}
