package com.soma369.laimory.core.util.permission

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 버전별 권한 조합 고정.
 *
 * Context 를 필요로 하는 판정은 Robolectric 없이 검증할 수 없어, SDK 로 갈리는 부분만 순수
 * 함수로 뽑아 두고 여기서 고정한다. 조합이 틀리면 예외가 아니라 **요청 목록에서 조용히 빠져**
 * 해당 데이터가 0건이 된다.
 */
class PermissionMatrixTest {
    @Test
    fun `사진 권한은 Android 14 이상에서 일부 허용 권한을 함께 요청한다`() {
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            ),
            PhotoPermission.required(Build.VERSION_CODES.UPSIDE_DOWN_CAKE).toList(),
        )
    }

    @Test
    fun `사진 권한은 Android 13 에서 이미지 전용 권한만 요청한다`() {
        assertEquals(
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.ACCESS_MEDIA_LOCATION),
            PhotoPermission.required(Build.VERSION_CODES.TIRAMISU).toList(),
        )
    }

    @Test
    fun `사진 권한은 Android 12 이하에서 저장소 읽기로 떨어진다`() {
        assertEquals(
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_MEDIA_LOCATION),
            PhotoPermission.required(Build.VERSION_CODES.S).toList(),
        )
    }

    @Test
    fun `일부 허용은 Android 14 이상에서만 성립한다`() {
        val partial =
            mapOf(
                Manifest.permission.READ_MEDIA_IMAGES to false,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED to true,
            )
        assertTrue(PhotoPermission.isLimited(partial, Build.VERSION_CODES.UPSIDE_DOWN_CAKE))
        assertFalse(PhotoPermission.isLimited(partial, Build.VERSION_CODES.TIRAMISU))
    }

    @Test
    fun `일부 허용도 사진을 읽을 수 있는 상태로 본다`() {
        // 사용자가 범위를 정한 것이지 거부한 것이 아니다.
        assertTrue(
            PhotoPermission.canRead(
                mapOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED to true),
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            ),
        )
    }

    @Test
    fun `앱 알림 권한은 Android 13 이상에서만 요청 대상이다`() {
        assertEquals(
            listOf(Manifest.permission.POST_NOTIFICATIONS),
            AppNotificationPermission.required(Build.VERSION_CODES.TIRAMISU).toList(),
        )
        assertTrue(AppNotificationPermission.required(Build.VERSION_CODES.S).isEmpty())
    }

    @Test
    fun `위치 1단계는 전경 위치와 활동 인식을 함께 요청한다`() {
        val tiramisu = LocationPermission.required(Build.VERSION_CODES.TIRAMISU).toList()
        assertEquals(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACTIVITY_RECOGNITION,
            ),
            tiramisu,
        )
    }

    @Test
    fun `위치 1단계에 백그라운드 위치를 섞지 않는다`() {
        // 전경과 함께 요청하면 시스템이 백그라운드 쪽을 조용히 거부한다.
        listOf(Build.VERSION_CODES.P, Build.VERSION_CODES.Q, Build.VERSION_CODES.TIRAMISU).forEach { sdk ->
            assertFalse(LocationPermission.required(sdk).contains(LocationPermission.background()))
        }
    }

    @Test
    fun `Android 9 는 활동 인식과 알림 권한을 요청하지 않는다`() {
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LocationPermission.required(Build.VERSION_CODES.P).toList(),
        )
    }
}
