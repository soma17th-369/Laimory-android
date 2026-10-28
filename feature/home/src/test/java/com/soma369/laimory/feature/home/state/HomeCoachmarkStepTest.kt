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
    fun `기록이 있는 날의 사진 단계는 사진 시트를 열겠다고 하지 않는다`() {
        val photo = HomeCoachmarkStep.PHOTO

        // 기록이 있는 날은 사진을 바꿀 수 없어 다음 단계에서 시트를 열지 않는다.
        assertNotEquals(photo.messageFor(hasRecord = false), photo.messageFor(hasRecord = true))
        assertEquals(photo.message, photo.messageFor(hasRecord = false))
    }

    @Test
    fun `다른 단계는 기록이 있어도 같은 문구다`() {
        HomeCoachmarkStep.entries.filter { it != HomeCoachmarkStep.CREATE && it != HomeCoachmarkStep.PHOTO }.forEach { step ->
            assertEquals(step.message, step.messageFor(hasRecord = true))
        }
    }
}
