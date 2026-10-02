package com.soma369.laimory.core.ui.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 위치 수집 고지 고정.
 *
 * 문구를 다듬다 Play 가 요구하는 요소 하나를 빼면 빌드는 통과하고 심사에서야 거부된다. 그 요소들이
 * 남아 있는지를 여기서 먼저 막는다.
 */
class LocationDisclosureTest {
    @Test
    fun `고지에 위치라는 말이 들어 있다`() {
        assertTrue(LocationDisclosure.BODY.contains("위치"))
    }

    @Test
    fun `고지가 앱을 쓰지 않을 때도 수집한다고 밝힌다`() {
        // Play 가 받는 표현: 백그라운드 · 앱이 종료되었을 때 · 항상 사용 중 · 앱이 사용되지 않을 때 중 하나.
        assertTrue(LocationDisclosure.BODY.contains("앱이 종료되었거나 사용 중이 아닐 때"))
    }

    @Test
    fun `고지가 위치를 쓰는 기능과 쓰임새를 밝힌다`() {
        assertTrue(LocationDisclosure.BODY.contains("하루 타임라인"))
        assertTrue(LocationDisclosure.BODY.contains("서버로 전송"))
    }

    @Test
    fun `받을 위치 권한이 남아 있으면 요청 전에 고지한다`() {
        assertTrue(LocationDisclosure.isNeededFor(LocationPermissionStep.FOREGROUND))
        assertTrue(LocationDisclosure.isNeededFor(LocationPermissionStep.BACKGROUND))
        assertTrue(LocationDisclosure.isNeededFor(LocationPermissionStep.ACTIVITY))
    }

    @Test
    fun `모두 받았으면 고지하지 않는다`() {
        assertFalse(LocationDisclosure.isNeededFor(LocationPermissionStep.GRANTED))
    }
}
