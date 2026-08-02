package com.soma369.laimory.core.domain.model.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SourceItemRetentionPolicyTest {
    @Test
    fun `30일 보존은 오늘을 포함한 최근 30개 날짜의 시작을 경계로 삼는다`() {
        val policy = policy(retentionDays = 30, now = "2026-08-02T12:00:00Z", zoneId = "Asia/Seoul")

        assertEquals(Instant.parse("2026-07-03T15:00:00Z"), policy.cutoff())
    }

    @Test
    fun `365일 보존은 오늘을 포함한 최근 365개 날짜의 시작을 경계로 삼는다`() {
        val policy = policy(retentionDays = 365, now = "2026-08-02T12:00:00Z", zoneId = "Asia/Seoul")

        assertEquals(Instant.parse("2025-08-02T15:00:00Z"), policy.cutoff())
    }

    @Test
    fun `DST 전환일도 24시간을 빼지 않고 현지 날짜 시작을 사용한다`() {
        val policy = policy(retentionDays = 2, now = "2026-03-09T12:00:00Z", zoneId = "America/New_York")

        assertEquals(Instant.parse("2026-03-08T05:00:00Z"), policy.cutoff())
    }

    @Test
    fun `cutoff를 계산할 때마다 최신 기기 시간대를 다시 읽는다`() {
        var currentZone = ZoneId.of("Asia/Seoul")
        val policy =
            SourceItemRetentionPolicy(
                config = SourceItemRetentionConfig(1),
                clock = Clock.fixed(Instant.parse("2026-08-02T23:30:00Z"), ZoneId.of("UTC")),
                zoneIdProvider = { currentZone },
            )

        assertEquals(Instant.parse("2026-08-02T15:00:00Z"), policy.cutoff())

        currentZone = ZoneId.of("America/Los_Angeles")
        assertEquals(Instant.parse("2026-08-02T07:00:00Z"), policy.cutoff())
    }

    @Test
    fun `보존 일수는 1일 이상이어야 한다`() {
        assertThrows(IllegalArgumentException::class.java) { SourceItemRetentionConfig(0) }
    }

    private fun policy(
        retentionDays: Int,
        now: String,
        zoneId: String,
    ) = SourceItemRetentionPolicy(
        config = SourceItemRetentionConfig(retentionDays),
        clock = Clock.fixed(Instant.parse(now), ZoneId.of("UTC")),
        zoneIdProvider = { ZoneId.of(zoneId) },
    )
}
