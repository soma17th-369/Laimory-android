package com.soma369.laimory.core.ui.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 수집 고지 문구와 띄우는 조건 고정.
 *
 * 문구를 다듬다 Play 가 요구하는 요소 하나를 빼면 빌드는 통과하고 심사에서야 거부된다. 그 요소들이
 * 남아 있는지를 여기서 먼저 막는다.
 */
class DataDisclosureTest {
    @Test
    fun `위치 고지에 위치라는 말과 앱을 쓰지 않을 때도 수집한다는 사실이 있다`() {
        val body = DataDisclosure.LOCATION.body
        assertTrue(body.contains("위치"))
        // Play 가 받는 표현: 백그라운드 · 앱이 종료되었을 때 · 항상 사용 중 · 앱이 사용되지 않을 때 중 하나.
        assertTrue(body.contains("앱이 종료되었거나 사용 중이 아닐 때"))
    }

    @Test
    fun `위치 고지가 기능과 쓰임새를 밝힌다`() {
        val body = DataDisclosure.LOCATION.body
        assertTrue(body.contains("하루 타임라인"))
        assertTrue(body.contains("서버로 전송"))
    }

    @Test
    fun `사진 고지가 촬영 위치를 읽고 보낸다고 밝힌다`() {
        // 사진 창은 촬영 위치를 말하지 않는다. 사용자가 알 수 있는 곳은 이 고지뿐이다.
        val body = DataDisclosure.PHOTO.body
        assertTrue(body.contains("촬영 위치"))
        assertTrue(body.contains("서버로 전송"))
    }

    @Test
    fun `받을 위치 권한이 남아 있으면 요청 전에 고지한다`() {
        listOf(
            LocationPermissionStep.FOREGROUND,
            LocationPermissionStep.BACKGROUND,
            LocationPermissionStep.ACTIVITY,
        ).forEach { step ->
            assertEquals(DataDisclosure.LOCATION, disclosureFor(DataPermission.LOCATION, locationStep = step))
        }
    }

    @Test
    fun `위치를 모두 받았으면 고지하지 않는다`() {
        assertNull(disclosureFor(DataPermission.LOCATION, locationStep = LocationPermissionStep.GRANTED))
    }

    @Test
    fun `사진은 아무것도 허용하지 않은 첫 요청에만 고지한다`() {
        assertEquals(DataDisclosure.PHOTO, disclosureFor(DataPermission.PHOTO, photoStatus = DataSourceStatus.DENIED))
    }

    @Test
    fun `사진을 일부라도 허용했으면 더 고르는 요청에 고지하지 않는다`() {
        // 촬영 위치 권한은 첫 허용 때 이미 받았다.
        assertNull(disclosureFor(DataPermission.PHOTO, photoStatus = DataSourceStatus.LIMITED))
        assertNull(disclosureFor(DataPermission.PHOTO, photoStatus = DataSourceStatus.GRANTED))
    }

    @Test
    fun `위치·사진 밖의 권한은 고지하지 않는다`() {
        listOf(
            DataPermission.CALENDAR,
            DataPermission.NOTIFICATION_LISTENER,
            DataPermission.APP_NOTIFICATION,
            DataPermission.HEALTH,
        ).forEach { permission ->
            assertNull(disclosureFor(permission))
        }
    }

    @Test
    fun `고지를 읽은 뒤 요청하는 권한이 고지 대상과 같다`() {
        assertEquals(DataPermission.LOCATION, DataDisclosure.LOCATION.permission)
        assertEquals(DataPermission.PHOTO, DataDisclosure.PHOTO.permission)
    }

    private fun disclosureFor(
        permission: DataPermission,
        locationStep: LocationPermissionStep = LocationPermissionStep.FOREGROUND,
        photoStatus: DataSourceStatus = DataSourceStatus.DENIED,
    ): DataDisclosure? = dataDisclosureBeforeRequest(permission, locationStep, photoStatus)
}
