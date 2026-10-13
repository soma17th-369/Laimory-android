package com.soma369.laimory.feature.home.loading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DraftLoadingStagePlanTest {
    private fun lengths(plan: DraftLoadingStagePlan): Map<DraftLoadingStage, Duration> {
        var start = Duration.ZERO
        return plan.deadlines.entries.sortedBy { it.value }.associate { (stage, end) ->
            (stage to (end - start)).also { start = end }
        }
    }

    private val seeds = (0L until 200L)

    @Test
    fun `고른 것이 없는 줄도 최소 시간은 돈다`() {
        seeds.forEach { seed ->
            lengths(DraftLoadingStagePlan.of(0, 0, 0, seed)).values.forEach { length ->
                assertTrue("$length", length >= DraftLoadingStagePlan.MIN_STAGE)
                assertTrue("$length", length <= DraftLoadingStagePlan.MIN_STAGE * (1 + DraftLoadingStagePlan.JITTER))
            }
        }
    }

    @Test
    fun `많이 고른 줄이 더 오래 돈다`() {
        seeds.forEach { seed ->
            val few = lengths(DraftLoadingStagePlan.of(photoCount = 1, calendarCount = 1, stayCount = 1, seed = seed))
            val many = lengths(DraftLoadingStagePlan.of(photoCount = 15, calendarCount = 6, stayCount = 8, seed = seed))
            DraftLoadingStage.entries.filter { it != DraftLoadingStage.AI }.forEach { stage ->
                assertTrue("$stage seed=$seed", many.getValue(stage) > few.getValue(stage))
            }
        }
    }

    @Test
    fun `아무리 많이 골라도 줄마다 상한을 넘지 않아 앞 세 줄은 1분 안에 끝난다`() {
        seeds.forEach { seed ->
            val plan = DraftLoadingStagePlan.of(photoCount = 500, calendarCount = 500, stayCount = 500, seed = seed)
            val photo = lengths(plan).getValue(DraftLoadingStage.PHOTO)
            assertTrue("$photo", photo <= 25.seconds)
            assertTrue("${plan.lastDeadline}", plan.lastDeadline <= 57.seconds)
        }
    }

    @Test
    fun `같은 씨앗이면 같은 계획이고 씨앗이 다르면 흔들린다`() {
        val a = DraftLoadingStagePlan.of(photoCount = 10, calendarCount = 3, stayCount = 4, seed = 42L)
        val b = DraftLoadingStagePlan.of(photoCount = 10, calendarCount = 3, stayCount = 4, seed = 42L)
        assertEquals(a, b)
        val others = (1L..20L).map { DraftLoadingStagePlan.of(photoCount = 10, calendarCount = 3, stayCount = 4, seed = it) }
        assertTrue(others.any { it != a })
        assertNotEquals(others.first().deadlines, others.last().deadlines)
    }

    @Test
    fun `흔들림은 이름값의 범위 안에 머문다`() {
        // 사진 10장: 4 + 1×10 = 14초
        seeds.forEach { seed ->
            val photo =
                lengths(
                    DraftLoadingStagePlan.of(photoCount = 10, calendarCount = 0, stayCount = 0, seed = seed),
                ).getValue(DraftLoadingStage.PHOTO)
            assertTrue("$photo", photo >= 14.seconds * (1 - DraftLoadingStagePlan.JITTER))
            assertTrue("$photo", photo <= 14.seconds * (1 + DraftLoadingStagePlan.JITTER))
        }
    }

    @Test
    fun `줄은 사진 일정 장소 순서로 끝난다`() {
        seeds.forEach { seed ->
            val d = DraftLoadingStagePlan.of(photoCount = 3, calendarCount = 2, stayCount = 5, seed = seed).deadlines
            assertTrue(d.getValue(DraftLoadingStage.PHOTO) < d.getValue(DraftLoadingStage.CALENDAR))
            assertTrue(d.getValue(DraftLoadingStage.CALENDAR) < d.getValue(DraftLoadingStage.STAY))
        }
    }
}
