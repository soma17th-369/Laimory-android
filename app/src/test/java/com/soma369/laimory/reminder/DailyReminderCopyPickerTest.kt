package com.soma369.laimory.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DailyReminderCopyPickerTest {
    private val picker = DailyReminderCopyPicker(Random(seed = 535))

    @Test
    fun `일반 문구는 전에 쓴 제목을 피한다`() {
        repeat(200) {
            val copy = picker.pickGeneral(titleCount = 4, bodyCount = 4, lastTitleIndex = 2)

            assertNotEquals(2, copy.titleIndex)
        }
    }

    @Test
    fun `오늘 하루가 겹치는 조합은 나오지 않는다`() {
        repeat(500) {
            val copy = picker.pickGeneral(titleCount = 4, bodyCount = 4, lastTitleIndex = null)

            assertTrue((copy.titleIndex to copy.bodyIndex) !in DailyReminderCopyPicker.EXCLUDED_GENERAL_PAIRS)
        }
    }

    @Test
    fun `제목과 본문은 섞여서 나온다`() {
        val pairs = List(500) { picker.pickGeneral(titleCount = 4, bodyCount = 4, lastTitleIndex = null) }.toSet()

        // 16 조합에서 제외 1개를 뺀 15 조합이 모두 나온다.
        assertEquals(15, pairs.size)
    }

    @Test
    fun `완료 문구는 짝 단위로 고르고 전에 쓴 짝을 피한다`() {
        repeat(200) {
            assertNotEquals(1, picker.pickCompleted(count = 4, lastIndex = 1).index)
        }
    }

    @Test
    fun `후보가 하나뿐이면 전에 쓴 것이라도 쓴다`() {
        assertEquals(0, picker.pickCompleted(count = 1, lastIndex = 0).index)
    }
}
