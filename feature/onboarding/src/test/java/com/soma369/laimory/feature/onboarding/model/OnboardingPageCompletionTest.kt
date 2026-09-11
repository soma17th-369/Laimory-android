package com.soma369.laimory.feature.onboarding.model

import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.DataPermissionState
import com.soma369.laimory.core.ui.permission.LocationPermissionStep
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 권한 장이 언제 끝나는지 고정한다.
 *
 * 이 판정이 넓으면 `항상 허용` 을 받기 전에 장이 끝나고, 좁으면 받을 것을 다 받았는데도 버튼이
 * 남아 한 번 더 눌러야 한다.
 */
class OnboardingPageCompletionTest {
    private fun state(
        locationStep: LocationPermissionStep,
        granted: Set<DataPermission> = emptySet(),
    ) = DataPermissionState(
        granted = granted,
        locationStep = locationStep,
        isPhotoLimited = false,
        hasListenerSettings = true,
        onRequest = {},
    )

    @Test
    fun `위치 장은 항상 허용까지 받으면 끝난다`() {
        assertTrue(state(LocationPermissionStep.GRANTED).isPageDone(DataPermission.LOCATION))
    }

    @Test
    fun `이동수단 인식만 거부해도 위치 장은 끝난 것으로 본다`() {
        // 첫 요청에서 이미 물었고, 없어도 속도 추론으로 수집은 돈다.
        assertTrue(state(LocationPermissionStep.ACTIVITY).isPageDone(DataPermission.LOCATION))
    }

    @Test
    fun `앱 사용 중에만이면 위치 장은 아직 끝나지 않았다`() {
        assertFalse(state(LocationPermissionStep.BACKGROUND).isPageDone(DataPermission.LOCATION))
        assertFalse(state(LocationPermissionStep.FOREGROUND).isPageDone(DataPermission.LOCATION))
    }

    @Test
    fun `다른 장은 허용 여부만 본다`() {
        val subject = state(LocationPermissionStep.FOREGROUND, granted = setOf(DataPermission.PHOTO))

        assertTrue(subject.isPageDone(DataPermission.PHOTO))
        assertFalse(subject.isPageDone(DataPermission.CALENDAR))
    }
}
