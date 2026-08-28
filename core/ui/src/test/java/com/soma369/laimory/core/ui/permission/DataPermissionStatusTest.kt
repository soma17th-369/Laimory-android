package com.soma369.laimory.core.ui.permission

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 설정 화면이 소스마다 다른 문구·버튼을 보여주는 근거를 고정한다.
 *
 * 여기서 검증하는 것은 "허용됐는가" 가 아니라 **어떤 상태로 보이고 무엇을 누르게 되는가** 다.
 * 두 값이 어긋나면 화면이 `허용 안 됨` 이라고 쓰면서 누를 버튼은 주지 않는 조합이 생긴다.
 */
class DataPermissionStatusTest {
    private fun state(
        granted: Set<DataPermission> = emptySet(),
        locationStep: LocationPermissionStep = LocationPermissionStep.FOREGROUND,
        isPhotoLimited: Boolean = false,
        hasListenerSettings: Boolean = true,
        needsSettingsForBackgroundLocation: Boolean = true,
    ) = DataPermissionState(
        granted = granted,
        locationStep = locationStep,
        isPhotoLimited = isPhotoLimited,
        hasListenerSettings = hasListenerSettings,
        needsSettingsForBackgroundLocation = needsSettingsForBackgroundLocation,
        onRequest = {},
    )

    @Test
    fun `일부 사진만 고른 상태는 허용도 거부도 아닌 제한으로 본다`() {
        val subject = state(granted = setOf(DataPermission.PHOTO), isPhotoLimited = true)

        assertEquals(DataSourceStatus.LIMITED, subject.statusOf(DataPermission.PHOTO))
        assertEquals(DataPermissionAction.RESELECT_PHOTOS, subject.actionFor(DataPermission.PHOTO))
    }

    @Test
    fun `사진을 전부 허용하면 더 누를 것이 없다`() {
        val subject = state(granted = setOf(DataPermission.PHOTO))

        assertEquals(DataSourceStatus.GRANTED, subject.statusOf(DataPermission.PHOTO))
        assertEquals(DataPermissionAction.NONE, subject.actionFor(DataPermission.PHOTO))
    }

    @Test
    fun `전경 위치만 열린 상태는 제한이며 Android 11 이상은 앱 설정으로 보낸다`() {
        val subject = state(locationStep = LocationPermissionStep.BACKGROUND)

        assertEquals(DataSourceStatus.LIMITED, subject.statusOf(DataPermission.LOCATION))
        assertEquals(DataPermissionAction.APP_SETTINGS, subject.actionFor(DataPermission.LOCATION))
    }

    @Test
    fun `Android 10 이하에서는 백그라운드 위치도 다이얼로그로 받는다`() {
        val subject =
            state(
                locationStep = LocationPermissionStep.BACKGROUND,
                needsSettingsForBackgroundLocation = false,
            )

        assertEquals(DataPermissionAction.REQUEST, subject.actionFor(DataPermission.LOCATION))
    }

    @Test
    fun `활동 인식만 빠진 위치도 제한이지만 다이얼로그로 다시 받을 수 있다`() {
        val subject = state(locationStep = LocationPermissionStep.ACTIVITY)

        assertEquals(DataSourceStatus.LIMITED, subject.statusOf(DataPermission.LOCATION))
        assertEquals(DataPermissionAction.REQUEST, subject.actionFor(DataPermission.LOCATION))
    }

    @Test
    fun `위치 권한이 아예 없으면 제한이 아니라 거부다`() {
        val subject = state(locationStep = LocationPermissionStep.FOREGROUND)

        assertEquals(DataSourceStatus.DENIED, subject.statusOf(DataPermission.LOCATION))
    }

    @Test
    fun `알림 읽기는 다이얼로그가 없어 설정 화면으로 보낸다`() {
        val subject = state()

        assertEquals(DataSourceStatus.DENIED, subject.statusOf(DataPermission.NOTIFICATION_LISTENER))
        assertEquals(DataPermissionAction.LISTENER_SETTINGS, subject.actionFor(DataPermission.NOTIFICATION_LISTENER))
    }

    @Test
    fun `알림 접근 설정이 없는 기기는 거부가 아니라 미지원으로 표시한다`() {
        // 켤 방법이 없는데 `허용 안 됨` 이라고 쓰면 사용자가 찾을 수 없는 화면을 찾아 헤맨다.
        val subject = state(hasListenerSettings = false)

        assertEquals(DataSourceStatus.UNSUPPORTED, subject.statusOf(DataPermission.NOTIFICATION_LISTENER))
        assertEquals(DataPermissionAction.NONE, subject.actionFor(DataPermission.NOTIFICATION_LISTENER))
    }

    @Test
    fun `캘린더는 허용 여부만 본다`() {
        assertEquals(DataSourceStatus.DENIED, state().statusOf(DataPermission.CALENDAR))
        assertEquals(
            DataSourceStatus.GRANTED,
            state(granted = setOf(DataPermission.CALENDAR)).statusOf(DataPermission.CALENDAR),
        )
    }
}
