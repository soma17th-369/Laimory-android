package com.soma369.laimory.core.ui.permission

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 위치 단계 판정 고정.
 *
 * 이 판정이 틀리면 예외가 아니라 버튼이 영영 같은 자리에 머물러, 눌러도 아무 일이 없는 화면이 된다.
 */
class LocationPermissionStepTest {
    @Test
    fun `전경이 없으면 전경부터 받는다`() {
        assertEquals(
            LocationPermissionStep.FOREGROUND,
            locationPermissionStep(hasForeground = false, hasBackground = false, hasActivityRecognition = false),
        )
    }

    @Test
    fun `대략 위치만 허용해도 전경 단계는 끝난 것으로 본다`() {
        // 정밀·대략 둘 다 요구하면 대략만 고른 사용자가 전경 단계에 갇힌다.
        assertEquals(
            LocationPermissionStep.BACKGROUND,
            locationPermissionStep(hasForeground = true, hasBackground = false, hasActivityRecognition = false),
        )
    }

    @Test
    fun `활동 인식만 거부하면 그 단계로 간다`() {
        // 전경 판정이 활동 인식까지 묶어 보면 여기 도달하지 못한다.
        assertEquals(
            LocationPermissionStep.ACTIVITY,
            locationPermissionStep(hasForeground = true, hasBackground = true, hasActivityRecognition = false),
        )
    }

    @Test
    fun `모두 허용이면 더 받을 것이 없다`() {
        assertEquals(
            LocationPermissionStep.GRANTED,
            locationPermissionStep(hasForeground = true, hasBackground = true, hasActivityRecognition = true),
        )
    }

    @Test
    fun `백그라운드가 없으면 활동 인식이 있어도 백그라운드가 먼저다`() {
        // 백그라운드가 위치 수집의 전제라 순서를 건너뛰지 않는다.
        assertEquals(
            LocationPermissionStep.BACKGROUND,
            locationPermissionStep(hasForeground = true, hasBackground = false, hasActivityRecognition = true),
        )
    }
}
