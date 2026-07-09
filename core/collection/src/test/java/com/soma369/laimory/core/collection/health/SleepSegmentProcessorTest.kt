package com.soma369.laimory.core.collection.health

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class SleepSegmentProcessorTest {
    private val selfPackage = "com.soma369.laimory"
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    // 수면 2026-07-09 23:00 ~ 07-10 07:00 KST → 밤 키(기상일)=2026-07-10
    private val startMillis = ZonedDateTime.of(2026, 7, 9, 23, 0, 0, 0, zone).toInstant().toEpochMilli()
    private val endMillis = ZonedDateTime.of(2026, 7, 10, 7, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun segment(isSuccessful: Boolean = true) = DetectedSleepSegment(startMillis, endMillis, isSuccessful)

    private fun processor(
        gateway: CapturingSleepHealthGateway,
        confidences: List<Int>,
    ) = SleepSegmentProcessor(
        SleepHealthRecorder(gateway, selfPackage),
        FakeSleepClassifyStore(confidences),
    ) { zone }

    @Test
    fun `성공·고신뢰도이고 외부 기록이 없으면 HC 에 기록한다`() =
        runTest {
            val gateway = CapturingSleepHealthGateway(selfPackage)
            processor(gateway, confidences = listOf(70, 80)).process(listOf(segment()))

            assertEquals(1, gateway.ownCount())
            val stored = gateway.read(Instant.ofEpochMilli(startMillis), Instant.ofEpochMilli(endMillis))
            assertEquals(Instant.ofEpochMilli(startMillis), stored[0].start)
            assertEquals(Instant.ofEpochMilli(endMillis), stored[0].end)
        }

    @Test
    fun `신뢰도가 낮으면 기록하지 않는다`() =
        runTest {
            val gateway = CapturingSleepHealthGateway(selfPackage)
            processor(gateway, confidences = listOf(10, 20)).process(listOf(segment()))

            assertEquals(0, gateway.ownCount())
        }

    @Test
    fun `status 가 성공이 아니면 기록하지 않는다`() =
        runTest {
            val gateway = CapturingSleepHealthGateway(selfPackage)
            processor(gateway, confidences = listOf(90, 95)).process(listOf(segment(isSuccessful = false)))

            assertEquals(0, gateway.ownCount())
        }

    @Test
    fun `그 밤에 외부 수면 기록이 있으면 기록하지 않는다`() =
        runTest {
            val gateway = CapturingSleepHealthGateway(selfPackage)
            gateway.seedExternal(
                StoredSleepSession(
                    start = Instant.ofEpochMilli(startMillis),
                    end = Instant.ofEpochMilli(endMillis),
                    clientRecordId = null,
                    originPackageName = "com.sec.android.app.shealth",
                ),
            )
            processor(gateway, confidences = listOf(70, 80)).process(listOf(segment()))

            assertEquals(0, gateway.ownCount())
        }
}

private class FakeSleepClassifyStore(
    private val confidences: List<Int>,
) : SleepClassifyStore {
    override suspend fun add(samples: List<SleepClassifySample>) = Unit

    override suspend fun confidencesInWindow(
        startMillis: Long,
        endMillis: Long,
    ): List<Int> = confidences
}

private class CapturingSleepHealthGateway(
    private val selfPackage: String,
) : SleepHealthGateway {
    private val own = mutableMapOf<String, SleepWrite>()
    private val external = mutableListOf<StoredSleepSession>()

    fun ownCount(): Int = own.size

    fun seedExternal(session: StoredSleepSession) {
        external += session
    }

    override suspend fun upsert(session: SleepWrite) {
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
}
