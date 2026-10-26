package com.soma369.laimory.feature.home.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HomeCoachmarkStepTest {
    @Test
    fun `기록이 있는 날의 만들기 단계는 열어 보라고 안내한다`() {
        val create = HomeCoachmarkStep.CREATE

        assertNotEquals(create.messageFor(hasRecord = false), create.messageFor(hasRecord = true))
        assertEquals(create.message, create.messageFor(hasRecord = false))
    }

    @Test
    fun `다른 단계는 기록이 있어도 같은 문구다`() {
        HomeCoachmarkStep.entries.filter { it != HomeCoachmarkStep.CREATE }.forEach { step ->
            assertEquals(step.message, step.messageFor(hasRecord = true))
        }
    }
}
