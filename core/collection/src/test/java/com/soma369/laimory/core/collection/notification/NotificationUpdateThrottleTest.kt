package com.soma369.laimory.core.collection.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class NotificationUpdateThrottleTest {
    private val throttle = NotificationUpdateThrottle()

    @Test
    fun `처음 보는 알림은 억제하지 않는다`() {
        assertFalse(throttle.isThrottled(KEY, BASE))
    }

    @Test
    fun `최소 간격 안에 다시 게시되면 억제한다`() {
        throttle.markCollected(KEY, BASE)

        assertTrue(throttle.isThrottled(KEY, BASE.plusMillis(150)))
        assertTrue(throttle.isThrottled(KEY, BASE.plusSeconds(59)))
    }

    @Test
    fun `최소 간격이 지나면 다시 수집한다`() {
        throttle.markCollected(KEY, BASE)

        assertFalse(throttle.isThrottled(KEY, BASE.plus(NotificationUpdateThrottle.MINIMUM_INTERVAL)))
    }

    @Test
    fun `다른 알림은 서로 억제하지 않는다`() {
        throttle.markCollected(KEY, BASE)

        assertFalse(throttle.isThrottled("com.example:0|com.example|2|null|10386", BASE.plusMillis(150)))
    }

    @Test
    fun `수집하지 않은 게시는 기록하지 않아 뒤늦은 수집을 막지 않는다`() {
        // 필터에 걸리지 않아 markCollected 를 부르지 않은 상태.
        assertFalse(throttle.isThrottled(KEY, BASE.plusMillis(150)))
    }

    @Test
    fun `기기 시간이 뒤로 돌아가면 억제하지 않는다`() {
        throttle.markCollected(KEY, BASE)

        assertFalse(throttle.isThrottled(KEY, BASE.minusSeconds(10)))
    }

    @Test
    fun `보관 한도를 넘으면 오래된 알림부터 잊는다`() {
        val bounded = NotificationUpdateThrottle(maxEntries = 2)
        bounded.markCollected("a", BASE)
        bounded.markCollected("b", BASE)
        bounded.markCollected("c", BASE)

        assertFalse(bounded.isThrottled("a", BASE.plusMillis(150)))
        assertTrue(bounded.isThrottled("c", BASE.plusMillis(150)))
    }

    @Test
    fun `억제 간격은 초 단위 갱신을 막고 분 단위 상태 전이는 통과시킨다`() {
        val interval = NotificationUpdateThrottle.MINIMUM_INTERVAL

        assertTrue(interval > Duration.ofSeconds(1))
        assertTrue(interval <= Duration.ofMinutes(1))
    }

    companion object {
        private val BASE: Instant = Instant.parse("2026-08-21T10:20:39Z")
        private const val KEY = "com.coupang.mobile:0|com.coupang.mobile|2101544574|null|10386"
    }
}
