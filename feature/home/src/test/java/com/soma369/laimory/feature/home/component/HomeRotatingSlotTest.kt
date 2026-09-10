package com.soma369.laimory.feature.home.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeRotatingSlotTest {
    @Test
    fun `자리와 값과 전체 건수를 한 덩어리로 낸다`() {
        val slot = listOf("가", "나", "다").rotatingSlotAt(1)

        assertEquals("나", slot?.value)
        assertEquals(1, slot?.index)
        assertEquals("2 / 3", slot?.positionLabel)
    }

    @Test
    fun `목록이 줄어 자리가 사라져도 터지지 않고 마지막 칸을 낸다`() {
        // 회전 위치는 3초마다 올라간 채 살아 있고, 수집이 갱신되면 목록이 줄어든다.
        // 전환 중에는 사라지는 칸이 한 프레임 더 그려지므로 이 조합이 실제로 들어온다.
        val slot = listOf("하나").rotatingSlotAt(1)

        assertEquals("하나", slot?.value)
        assertEquals(0, slot?.index)
        assertEquals("1 / 1", slot?.positionLabel)
    }

    @Test
    fun `빈 목록에는 그릴 칸이 없다`() {
        assertNull(emptyList<String>().rotatingSlotAt(0))
    }
}
