package com.soma369.laimory.core.collection.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class NotificationUpdateThrottleTest {
    private val throttle = NotificationUpdateThrottle()

    @Test
    fun `처음 보는 알림은 억제하지 않는다`() {
        assertFalse(throttle.onPosted(KEY, BASE))
    }

    @Test
    fun `최소 간격 안에 다시 게시되면 억제한다`() {
        collect(BASE)

        assertTrue(throttle.onPosted(KEY, BASE.plusMillis(150)))
        assertTrue(throttle.onPosted(KEY, BASE.plusSeconds(59)))
    }

    @Test
    fun `최소 간격이 지나면 다시 수집한다`() {
        collect(BASE)

        assertFalse(throttle.onPosted(KEY, BASE.plus(NotificationUpdateThrottle.MINIMUM_INTERVAL)))
    }

    @Test
    fun `수집하지 않은 게시는 뒤늦은 수집을 막지 않는다`() {
        // 필터에 걸리지 않아 onCollected 를 부르지 않은 상태.
        assertFalse(throttle.onPosted(KEY, BASE))

        assertFalse(throttle.onPosted(KEY, BASE.plusMillis(150)))
    }

    @Test
    fun `다른 알림은 서로 억제하지 않는다`() {
        collect(BASE)

        assertFalse(throttle.onPosted("com.example:0|com.example|2|null|10386", BASE.plusMillis(150)))
    }

    @Test
    fun `기기 시간이 뒤로 돌아가면 억제하지 않는다`() {
        collect(BASE)

        assertFalse(throttle.onPosted(KEY, BASE.minusSeconds(10)))
    }

    // --- 갱신 에피소드 건수 상한 ---

    @Test
    fun `초당 갱신이 재현 시간 내내 이어져도 에피소드 상한에서 멈춘다`() {
        // 이슈 #280 재현 조건 그대로: 초당 1건씩 2시간 27분(8,820초).
        val burst = 8_820L

        val collected = simulate(postCount = burst + 1, postInterval = Duration.ofSeconds(1))

        assertEquals(NotificationUpdateThrottle.MAX_COLLECTIONS_PER_EPISODE, collected)
        // 간격만 있던 시절의 8,820 / 60 ≈ 147건, 억제가 없던 시절의 6,126건과 대비된다.
        assertTrue(collected < DRAFT_NOTIFICATION_LIMIT / 10 + 1)
    }

    @Test
    fun `억제되는 동안에도 게시가 이어지면 에피소드가 끊기지 않는다`() {
        val idleGap = NotificationUpdateThrottle.EPISODE_IDLE_GAP

        // 상한까지 채운 뒤, 유휴 간격보다 촘촘히 계속 게시한다.
        val collected = simulate(postCount = 200, postInterval = idleGap.dividedBy(2))

        assertEquals(NotificationUpdateThrottle.MAX_COLLECTIONS_PER_EPISODE, collected)
    }

    @Test
    fun `게시가 유휴 간격만큼 끊기면 새 에피소드로 다시 수집한다`() {
        repeat(NotificationUpdateThrottle.MAX_COLLECTIONS_PER_EPISODE) { index ->
            collect(BASE.plus(NotificationUpdateThrottle.MINIMUM_INTERVAL.multipliedBy(index.toLong())))
        }
        val exhaustedAt = BASE.plus(NotificationUpdateThrottle.MINIMUM_INTERVAL.multipliedBy(10))
        assertTrue(throttle.onPosted(KEY, exhaustedAt))

        val afterIdle = exhaustedAt.plus(NotificationUpdateThrottle.EPISODE_IDLE_GAP)

        assertFalse(throttle.onPosted(KEY, afterIdle))
    }

    @Test
    fun `정상 앱의 상태 전이 알림은 한 에피소드에서 모두 수집한다`() {
        // 주문 접수 → 조리 시작 → 픽업 → 배달 중 → 완료. 같은 알림 id 를 5분 간격으로 갱신한다.
        val transitions = 5

        val collected = simulate(postCount = transitions.toLong(), postInterval = Duration.ofMinutes(5))

        assertEquals(transitions, collected)
    }

    // --- 알림 제거 ---

    @Test
    fun `알림이 제거되면 같은 키를 즉시 재사용해도 억제하지 않는다`() {
        collect(BASE)
        assertTrue(throttle.onPosted(KEY, BASE.plusMillis(150)))

        throttle.forget(KEY)

        assertFalse(throttle.onPosted(KEY, BASE.plusMillis(300)))
    }

    @Test
    fun `알림이 제거되면 에피소드 건수도 초기화한다`() {
        repeat(NotificationUpdateThrottle.MAX_COLLECTIONS_PER_EPISODE) { index ->
            collect(BASE.plus(NotificationUpdateThrottle.MINIMUM_INTERVAL.multipliedBy(index.toLong())))
        }
        val exhaustedAt = BASE.plus(NotificationUpdateThrottle.MINIMUM_INTERVAL.multipliedBy(10))
        assertTrue(throttle.onPosted(KEY, exhaustedAt))

        throttle.forget(KEY)

        assertFalse(throttle.onPosted(KEY, exhaustedAt.plusSeconds(1)))
    }

    // --- 보관 한도 ---

    @Test
    fun `보관 한도를 넘으면 오래된 알림부터 잊는다`() {
        val bounded = NotificationUpdateThrottle(maxEntries = 2)
        listOf("a", "b", "c").forEach { key ->
            bounded.onPosted(key, BASE)
            bounded.onCollected(key, BASE)
        }

        assertFalse(bounded.onPosted("a", BASE.plusMillis(150)))
        assertTrue(bounded.onPosted("c", BASE.plusMillis(150)))
    }

    /** [postCount] 건을 [postInterval] 간격으로 게시하고 실제로 수집된 건수를 센다. */
    private fun simulate(
        postCount: Long,
        postInterval: Duration,
    ): Int {
        var collected = 0
        for (index in 0 until postCount) {
            val at = BASE.plus(postInterval.multipliedBy(index))
            if (throttle.onPosted(KEY, at)) continue
            throttle.onCollected(KEY, at)
            collected++
        }
        return collected
    }

    private fun collect(at: Instant) {
        throttle.onPosted(KEY, at)
        throttle.onCollected(KEY, at)
    }

    companion object {
        private val BASE: Instant = Instant.parse("2026-08-21T10:20:39Z")
        private const val KEY = "com.coupang.mobile:0|com.coupang.mobile|2101544574|null|10386"

        /** `DraftSourceItemSelectionPolicy` 의 알림 타입 상한. */
        private const val DRAFT_NOTIFICATION_LIMIT = 100
    }
}
