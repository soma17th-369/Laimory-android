package com.soma369.laimory.core.collection.collector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class CalendarInstanceIdentityTest {
    @Test
    fun `같은 인스턴스는 읽을 때마다 같은 rawId 다`() {
        assertEquals(
            CalendarInstanceIdentity.rawIdOf(eventId = 42, begin = 1_760_000_000_000),
            CalendarInstanceIdentity.rawIdOf(eventId = 42, begin = 1_760_000_000_000),
        )
    }

    @Test
    fun `반복 일정의 다른 회차와 다른 일정은 rawId 가 다르다`() {
        val base = CalendarInstanceIdentity.rawIdOf(eventId = 42, begin = 1_760_000_000_000)

        assertNotEquals(base, CalendarInstanceIdentity.rawIdOf(eventId = 42, begin = 1_760_086_400_000))
        assertNotEquals(base, CalendarInstanceIdentity.rawIdOf(eventId = 43, begin = 1_760_000_000_000))
    }

    @Test
    fun `rawId 는 서버가 받는 canonical lowercase UUID 다`() {
        val rawId = CalendarInstanceIdentity.rawIdOf(eventId = 42, begin = 1_760_000_000_000)

        assertTrue(rawId, rawId.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
    }

    @Test
    fun `sourceKey 는 eventId 와 원본 begin 으로 만든다`() {
        assertEquals("42:1760000000000", CalendarInstanceIdentity.sourceKeyOf(eventId = 42, begin = 1_760_000_000_000))
    }
}
