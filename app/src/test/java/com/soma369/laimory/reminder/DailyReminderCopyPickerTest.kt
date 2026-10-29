package com.soma369.laimory.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class DailyReminderCopyPickerTest {
    private val picker = DailyReminderCopyPicker(Random(seed = 535))

    @Test
    fun `일반 문구는 전에 쓴 짝을 피한다`() {
        repeat(200) {
            assertNotEquals(2, picker.pickGeneral(count = 4, lastIndex = 2).index)
        }
    }

    @Test
    fun `일반 문구는 네 짝이 모두 나온다`() {
        val indices = List(200) { picker.pickGeneral(count = 4, lastIndex = null).index }.toSet()

        assertEquals(setOf(0, 1, 2, 3), indices)
    }

    @Test
    fun `완료 문구는 전에 쓴 짝을 피한다`() {
        repeat(200) {
            assertNotEquals(1, picker.pickCompleted(count = 4, lastIndex = 1).index)
        }
    }

    @Test
    fun `후보가 하나뿐이면 전에 쓴 것이라도 쓴다`() {
        assertEquals(0, picker.pickCompleted(count = 1, lastIndex = 0).index)
    }
}
