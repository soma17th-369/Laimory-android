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
        isHealthAvailable: Boolean = true,
        blocked: Set<DataPermission> = emptySet(),
    ) = DataPermissionState(
        granted = granted,
        locationStep = locationStep,
        isPhotoLimited = isPhotoLimited,
        hasListenerSettings = hasListenerSettings,
        needsSettingsForBackgroundLocation = needsSettingsForBackgroundLocation,
        isHealthAvailable = isHealthAvailable,
        blocked = blocked,
        onRequest = {},
    )

    @Test
    fun `일부 사진만 고른 상태는 허용도 거부도 아닌 제한으로 본다`() {
        val subject = state(granted = setOf(DataPermission.PHOTO), isPhotoLimited = true)

        assertEquals(DataSourceStatus.LIMITED, subject.statusOf(DataPermission.PHOTO))
        assertEquals(DataPermissionAction.RESELECT_PHOTOS, subject.actionFor(DataPermission.PHOTO))
    }

    @Test
    fun `이미 허용된 소스도 설정으로 보내 끄거나 좁힐 수 있게 한다`() {
        // 설정 화면에 들어오는 이유의 절반은 끄려는 것이다. 막다른 길로 두지 않는다.
        val subject = state(granted = setOf(DataPermission.PHOTO))

        assertEquals(DataSourceStatus.GRANTED, subject.statusOf(DataPermission.PHOTO))
        assertEquals(DataPermissionAction.APP_SETTINGS, subject.actionFor(DataPermission.PHOTO))
    }

    @Test
    fun `허용된 헬스는 앱 설정이 아니라 Health Connect 로 보낸다`() {
        // 헬스 권한은 앱 상세 설정에 나오지 않는다.
        val subject = state(granted = setOf(DataPermission.HEALTH))

        assertEquals(DataPermissionAction.HEALTH_SETTINGS, subject.actionFor(DataPermission.HEALTH))
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
    fun `시스템이 더 이상 묻지 않는 권한은 허용하기 대신 설정으로 보낸다`() {
        // 두 번 거부한 뒤에는 요청을 보내도 다이얼로그가 뜨지 않는다. 그대로 두면 눌러도
        // 아무 일이 없는 버튼이 된다.
        val subject = state(blocked = setOf(DataPermission.PHOTO))

        assertEquals(DataSourceStatus.DENIED, subject.statusOf(DataPermission.PHOTO))
        assertEquals(DataPermissionAction.APP_SETTINGS, subject.actionFor(DataPermission.PHOTO))
    }

    @Test
    fun `아직 물어볼 수 있는 권한은 다이얼로그를 띄운다`() {
        val subject = state()

        assertEquals(DataPermissionAction.REQUEST, subject.actionFor(DataPermission.PHOTO))
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
    fun `Health Connect 가 없는 기기는 거부가 아니라 미지원이다`() {
        // 허용/거부 이전의 문제라 `허용하기` 버튼을 줘도 열리는 화면이 없다.
        val subject = state(isHealthAvailable = false)

        assertEquals(DataSourceStatus.UNSUPPORTED, subject.statusOf(DataPermission.HEALTH))
        assertEquals(DataPermissionAction.NONE, subject.actionFor(DataPermission.HEALTH))
    }

    @Test
    fun `Health Connect 가 있으면 허용 여부를 묻는다`() {
        val subject = state()

        assertEquals(DataSourceStatus.DENIED, subject.statusOf(DataPermission.HEALTH))
        assertEquals(DataPermissionAction.REQUEST, subject.actionFor(DataPermission.HEALTH))
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
