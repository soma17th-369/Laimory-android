package com.soma369.laimory.core.collection.health

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SleepHealthRecorderTest {
    private val selfPackage = "com.soma369.laimory"
    private val night: LocalDate = LocalDate.of(2026, 7, 9)

    /** 2026-07-09 22:00 ~ 07-10 06:00 KST 를 UTC instant 로. */
    private val start: Instant = Instant.parse("2026-07-09T13:00:00Z")
    private val end: Instant = Instant.parse("2026-07-09T21:00:00Z")

    private fun recorder(gateway: SleepHealthGateway) = SleepHealthRecorder(gateway, selfPackage)

    @Test
    fun `감지 수면을 쓰면 우리 패키지 세션으로 다시 읽힌다`() =
        runTest {
            val gateway = FakeSleepHealthGateway(selfPackage)
            recorder(gateway).recordDetectedSleep(night, start, end, ZoneOffset.UTC)

            val sessions = gateway.read(start, end)
            assertEquals(1, sessions.size)
            assertEquals(start, sessions[0].start)
            assertEquals(end, sessions[0].end)
            assertEquals(selfPackage, sessions[0].originPackageName)
            assertEquals(SleepRecordingMethod.AUTO_DETECTED, gateway.lastWrite?.recordingMethod)
        }

    @Test
    fun `같은 밤 재기록은 중복 세션을 만들지 않고 갱신한다`() =
        runTest {
            val gateway = FakeSleepHealthGateway(selfPackage)
            val recorder = recorder(gateway)
            recorder.recordDetectedSleep(night, start, end, ZoneOffset.UTC)
            recorder.recordDetectedSleep(night, start, end.plusSeconds(600), ZoneOffset.UTC)

            val sessions = gateway.read(start, end.plusSeconds(600))
            assertEquals(1, sessions.size)
            // 나중 기록(더 큰 version)이 반영된다.
            assertEquals(end.plusSeconds(600), sessions[0].end)
        }

    @Test
    fun `삭제하면 그 밤 세션이 사라진다`() =
        runTest {
            val gateway = FakeSleepHealthGateway(selfPackage)
            val recorder = recorder(gateway)
            recorder.recordDetectedSleep(night, start, end, ZoneOffset.UTC)
            recorder.deleteRecordedSleep(night)

            assertTrue(gateway.read(start, end).isEmpty())
        }

    @Test
    fun `외부 앱 수면이 겹치면 hasExternalSleep 이 true`() =
        runTest {
            val gateway = FakeSleepHealthGateway(selfPackage)
            gateway.seedExternal(
                StoredSleepSession(start, end, clientRecordId = null, originPackageName = "com.sec.android.app.shealth"),
            )

            assertTrue(recorder(gateway).hasExternalSleep(start, end))
        }

    @Test
    fun `우리 세션만 있으면 hasExternalSleep 이 false`() =
        runTest {
            val gateway = FakeSleepHealthGateway(selfPackage)
            val recorder = recorder(gateway)
            recorder.recordDetectedSleep(night, start, end, ZoneOffset.UTC)

            assertFalse(recorder.hasExternalSleep(start, end))
        }

    @Test
    fun `수동 입력은 MANUAL 로 기록된다`() =
        runTest {
            val gateway = FakeSleepHealthGateway(selfPackage)
            recorder(gateway).recordManualSleep(night, start, end, ZoneOffset.UTC)

            assertEquals(SleepRecordingMethod.MANUAL, gateway.lastWrite?.recordingMethod)
        }
}

/**
 * HC 대체 인메모리 gateway.
 * upsert=clientRecordId 키로 교체(version 이 기존 이상일 때), read=구간 overlap, 우리 기록은 [selfPackage] 로 스탬프.
 */
private class FakeSleepHealthGateway(
    private val selfPackage: String,
) : SleepHealthGateway {
    private val own = mutableMapOf<String, SleepWrite>()
    private val external = mutableListOf<StoredSleepSession>()

    var lastWrite: SleepWrite? = null
        private set

    override suspend fun upsert(session: SleepWrite) {
        lastWrite = session
        val existing = own[session.clientRecordId]
        if (existing == null || session.version >= existing.version) {
            own[session.clientRecordId] = session
        }
    }

    override suspend fun deleteByClientRecordId(clientRecordId: String) {
        own.remove(clientRecordId)
    }

    override suspend fun read(
        start: Instant,
        end: Instant,
    ): List<StoredSleepSession> {
        val ours = own.values.map { StoredSleepSession(it.start, it.end, it.clientRecordId, selfPackage) }
        return (ours + external).filter { it.start < end && it.end > start }
    }

    fun seedExternal(session: StoredSleepSession) {
        external += session
    }
}
