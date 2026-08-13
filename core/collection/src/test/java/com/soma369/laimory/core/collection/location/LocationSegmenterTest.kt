package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSegmenterTest {
    private fun segmenter(
        dwellRadiusMeters: Double = 80.0,
        stayMillis: Long = minute,
        maxSampleGapMillis: Long = 5 * minute,
        requiredConsecutiveOutsideSamples: Int = 2,
        minMovementDurationMillis: Long = minMovement,
        initialSnapshot: LocationSegmentSnapshot? = null,
    ): LocationSegmenter {
        var nextId = 0
        return LocationSegmenter(
            dwellRadiusMeters = dwellRadiusMeters,
            stayMillis = stayMillis,
            maxSampleGapMillis = maxSampleGapMillis,
            requiredConsecutiveOutsideSamples = requiredConsecutiveOutsideSamples,
            minMovementDurationMillis = minMovementDurationMillis,
            initialSnapshot = initialSnapshot,
            rawIdFactory = { "raw-${nextId++}" },
        )
    }

    private val minute = 60_000L

    // 운영 기본값(20분)은 축소해 검증한다 — 실제 상수는 별도 테스트가 고정한다.
    private val minMovement = 2 * minute
    private val placeLat = 37.5000
    private val placeLng = 127.0000

    @Test
    fun `haversine 거리 계산이 대략 맞다`() {
        val distance = LocationSegmenter.distanceMeters(37.5, 127.0, 37.545, 127.0)

        assertTrue("expected ~5000m but was $distance", distance in 4800.0..5200.0)
    }

    @Test
    fun `체류 인정 시간에 도달하면 열린 Dwell을 즉시 확정하고 같은 rawId로 갱신한다`() {
        val segmenter = segmenter()

        assertTrue(segmenter.onSample(placeLat, placeLng, 0L, 20.0).isEmpty())
        val confirmed = segmenter.onSample(placeLat, placeLng, minute, 20.0).single() as DetectedEvent.Dwell
        val updated = segmenter.onSample(placeLat, placeLng, 2 * minute, 20.0).single() as DetectedEvent.Dwell

        assertEquals(confirmed.rawId, updated.rawId)
        assertEquals(0L, confirmed.startMillis)
        assertEquals(minute, confirmed.endMillis)
        assertEquals(2 * minute, updated.endMillis)
    }

    @Test
    fun `한 장소를 두 샘플 연속 벗어나야 체류를 마감하고 이동을 시작한다`() {
        val segmenter = segmenter()
        segmenter.onSample(placeLat, placeLng, 0L)
        segmenter.onSample(placeLat, placeLng, minute)
        segmenter.onSample(placeLat, placeLng, 2 * minute)

        val firstOutside = segmenter.onSample(37.545, 127.0, 3 * minute)
        val statusAfterFirst = segmenter.currentStatus(3 * minute)
        val secondOutside = segmenter.onSample(37.5451, 127.0, 3 * minute + 30_000L)

        assertTrue(firstOutside.isEmpty())
        assertTrue(statusAfterFirst is LocationTrackingStatus.Dwelling)
        val dwell = secondOutside.filterIsInstance<DetectedEvent.Dwell>().single()
        assertEquals(2 * minute, dwell.endMillis)
        assertTrue(segmenter.currentStatus(3 * minute + 30_000L) is LocationTrackingStatus.Moving)
    }

    @Test
    fun `단발성 반경 이탈 뒤 복귀하면 같은 체류를 이어간다`() {
        val segmenter = segmenter()
        segmenter.onSample(placeLat, placeLng, 0L)
        val firstConfirmed = segmenter.onSample(placeLat, placeLng, minute).single() as DetectedEvent.Dwell

        assertTrue(segmenter.onSample(37.545, 127.0, minute + 30_000L).isEmpty())
        val resumed = segmenter.onSample(placeLat, placeLng, 2 * minute).single() as DetectedEvent.Dwell

        assertEquals(firstConfirmed.rawId, resumed.rawId)
        assertTrue(segmenter.currentStatus(2 * minute) is LocationTrackingStatus.Dwelling)
    }

    @Test
    fun `체류에서 빠르게 이동해 새 장소에 안착하면 Move와 열린 Dwell을 함께 낸다`() {
        val segmenter = segmenter()
        segmenter.onSample(placeLat, placeLng, 0L)
        segmenter.onSample(placeLat, placeLng, minute)
        segmenter.onSample(placeLat, placeLng, 2 * minute)

        val destinationLat = 37.54515
        // 이동 확정 시간(endAt - startAt)이 최소 지속 시간 이상이 되도록 이탈 시각을 둔다.
        segmenter.onSample(destinationLat, placeLng, 4 * minute)
        segmenter.onSample(destinationLat, placeLng, 4 * minute + 30_000L)
        val arrival = segmenter.onSample(destinationLat, placeLng, 5 * minute + 30_000L)

        val move = arrival.filterIsInstance<DetectedEvent.Move>().single()
        val dwell = arrival.filterIsInstance<DetectedEvent.Dwell>().single()
        assertEquals(placeLat, move.startLatitude, 1e-9)
        assertEquals(destinationLat, move.endLatitude, 1e-9)
        assertEquals(MovementPayload.Transport.IN_VEHICLE, move.transport)
        assertTrue("distance ~5km", move.distanceMeters in 4800.0..5300.0)
        assertEquals(minMovement, move.endMillis - move.startMillis)
        assertEquals(4 * minute, dwell.startMillis)
        assertEquals(5 * minute + 30_000L, dwell.endMillis)
    }

    @Test
    fun `정확도 가중 평균으로 체류 대표 좌표를 계산한다`() {
        val segmenter = segmenter()
        val secondLatitude = placeLat + 0.0001

        segmenter.onSample(placeLat, placeLng, 0L, accuracyMeters = 10.0)
        val dwell =
            segmenter.onSample(secondLatitude, placeLng, minute, accuracyMeters = 20.0).single() as DetectedEvent.Dwell

        val expected = (placeLat * 4.0 + secondLatitude) / 5.0
        assertEquals(expected, dwell.latitude, 1e-9)
    }

    @Test
    fun `정확도 100m를 초과한 샘플은 상태와 대표 좌표에서 제외한다`() {
        val segmenter = segmenter()
        segmenter.onSample(placeLat, placeLng, 0L, accuracyMeters = 20.0)

        assertTrue(segmenter.onSample(37.545, 127.0, 30_000L, accuracyMeters = 101.0).isEmpty())
        val dwell = segmenter.onSample(placeLat, placeLng, minute, accuracyMeters = 20.0).single() as DetectedEvent.Dwell

        assertEquals(placeLat, dwell.latitude, 1e-9)
        assertEquals(placeLng, dwell.longitude, 1e-9)
    }

    @Test
    fun `5분을 초과한 복원 공백은 확정 체류를 마지막 샘플에서 닫고 새 구간을 시작한다`() {
        val segmenter = segmenter()
        segmenter.onSample(placeLat, placeLng, 0L)
        val confirmed = segmenter.onSample(placeLat, placeLng, minute).single() as DetectedEvent.Dwell

        val closed = segmenter.onSample(placeLat, placeLng, 7 * minute).single() as DetectedEvent.Dwell
        val newState = segmenter.snapshot()?.state as LocationSegmentState.AtPlace

        assertEquals(confirmed.rawId, closed.rawId)
        assertEquals(minute, closed.endMillis)
        assertNotEquals(confirmed.rawId, newState.place.rawId)
        assertTrue(!newState.confirmed)
    }

    @Test
    fun `5분을 초과한 공백의 미확정 체류 후보는 이벤트 없이 폐기한다`() {
        val segmenter = segmenter()
        segmenter.onSample(placeLat, placeLng, 0L)
        val originalRawId = (segmenter.snapshot()?.state as LocationSegmentState.AtPlace).place.rawId

        val events = segmenter.onSample(placeLat, placeLng, 6 * minute)
        val restarted = segmenter.snapshot()?.state as LocationSegmentState.AtPlace

        assertTrue(events.isEmpty())
        assertNotEquals(originalRawId, restarted.place.rawId)
        assertEquals(6 * minute, restarted.place.sinceMillis)
    }

    @Test
    fun `스냅샷을 복원하면 같은 rawId의 체류 후보를 이어간다`() {
        val original = segmenter()
        original.onSample(placeLat, placeLng, 0L)
        original.onSample(placeLat, placeLng, 30_000L)
        val restoredSnapshot = LocationSegmentSnapshotCodec.decode(LocationSegmentSnapshotCodec.encode(original.snapshot()!!))
        val restored = segmenter(initialSnapshot = restoredSnapshot)

        val dwell = restored.onSample(placeLat, placeLng, minute).single() as DetectedEvent.Dwell
        val originalState = restoredSnapshot.state as LocationSegmentState.AtPlace

        assertEquals(originalState.place.rawId, dwell.rawId)
        assertEquals(0L, dwell.startMillis)
    }

    @Test
    fun `이동 중 스냅샷을 복원하면 누적 거리와 이동 rawId를 이어간다`() {
        val original = segmenter(stayMillis = 5 * minute)
        original.onSample(placeLat, placeLng, 0L)
        original.onSample(37.545, placeLng, minute)
        original.onSample(37.5451, placeLng, minute + 30_000L)
        val traveling = original.snapshot()!!
        val travelingState = traveling.state as LocationSegmentState.Traveling
        val restored = segmenter(stayMillis = 5 * minute, initialSnapshot = traveling)

        restored.onSample(37.5452, placeLng, 2 * minute)
        val resumed = restored.snapshot()?.state as LocationSegmentState.Traveling

        assertEquals(travelingState.rawId, resumed.rawId)
        assertTrue(resumed.totalDistanceMeters >= travelingState.totalDistanceMeters)
    }

    @Test
    fun `최소 지속 시간 미만 이동은 추적 종료 시에도 저장하지 않는다`() {
        val segmenter = segmenter()
        segmenter.onSample(placeLat, placeLng, 0L)
        segmenter.onSample(37.545, placeLng, 30_000L)
        segmenter.onSample(37.5451, placeLng, minute)

        // 이동 확정 시간 1분 < 최소 지속 시간 2분.
        assertTrue(segmenter.flush().isEmpty())
        assertEquals(null, segmenter.snapshot())
    }

    @Test
    fun `최소 지속 시간 이상 이동은 추적 종료 시 저장한다`() {
        val segmenter = segmenter()
        segmenter.onSample(placeLat, placeLng, 0L)
        segmenter.onSample(37.545, placeLng, 30_000L)
        segmenter.onSample(37.5451, placeLng, minute)
        segmenter.onSample(37.56, placeLng, 3 * minute)

        val move = segmenter.flush().single() as DetectedEvent.Move

        assertEquals(0L, move.startMillis)
        assertEquals(3 * minute, move.endMillis)
    }

    @Test
    fun `정상 도착 이동이 최소 지속 시간에 미달하면 이동만 버리고 체류는 확정한다`() {
        val segmenter = segmenter()
        segmenter.onSample(placeLat, placeLng, 0L)
        segmenter.onSample(placeLat, placeLng, minute)

        val destinationLat = 37.54515
        // 이동 확정 시간 1분 < 최소 지속 시간 2분.
        segmenter.onSample(destinationLat, placeLng, 2 * minute)
        segmenter.onSample(destinationLat, placeLng, 2 * minute + 30_000L)
        val arrival = segmenter.onSample(destinationLat, placeLng, 3 * minute + 30_000L)

        assertTrue(arrival.filterIsInstance<DetectedEvent.Move>().isEmpty())
        val dwell = arrival.filterIsInstance<DetectedEvent.Dwell>().single()
        assertEquals(2 * minute, dwell.startMillis)
        // 짧은 이동을 버려도 도착 체류와 진행 상태는 정상 확정된다.
        val state = segmenter.snapshot()?.state as LocationSegmentState.AtPlace
        assertTrue(state.confirmed)
        assertEquals(dwell.rawId, state.place.rawId)
    }

    @Test
    fun `정상 도착 이동은 최소 지속 시간 경계값에서 저장한다`() {
        val boundary = segmenter().arriveAfterTravel(travelMillis = minMovement)
        val justBelow = segmenter().arriveAfterTravel(travelMillis = minMovement - 1)

        assertEquals(minMovement, boundary.filterIsInstance<DetectedEvent.Move>().single().let { it.endMillis - it.startMillis })
        assertTrue(justBelow.filterIsInstance<DetectedEvent.Move>().isEmpty())
    }

    @Test
    fun `샘플 공백으로 닫는 이동도 최소 지속 시간 기준을 따른다`() {
        val shortSegmenter = segmenter()
        shortSegmenter.onSample(placeLat, placeLng, 0L)
        shortSegmenter.onSample(placeLat, placeLng, minute)
        shortSegmenter.onSample(37.545, placeLng, 2 * minute)
        shortSegmenter.onSample(37.5451, placeLng, 2 * minute + 30_000L)
        // 이동 확정 시간 1분 30초 < 2분.
        val closedShort = shortSegmenter.onSample(placeLat, placeLng, 9 * minute)

        val longSegmenter = segmenter()
        longSegmenter.onSample(placeLat, placeLng, 0L)
        longSegmenter.onSample(placeLat, placeLng, minute)
        longSegmenter.onSample(37.545, placeLng, 2 * minute)
        longSegmenter.onSample(37.5451, placeLng, 2 * minute + 30_000L)
        longSegmenter.onSample(37.56, placeLng, 4 * minute)
        // 이동 확정 시간 3분 >= 2분.
        val closedLong = longSegmenter.onSample(placeLat, placeLng, 10 * minute)

        assertTrue(closedShort.filterIsInstance<DetectedEvent.Move>().isEmpty())
        val move = closedLong.filterIsInstance<DetectedEvent.Move>().single()
        assertEquals(minute, move.startMillis)
        assertEquals(4 * minute, move.endMillis)
    }

    @Test
    fun `복원한 이동 상태를 마감할 때도 최소 지속 시간 기준을 따른다`() {
        val original = segmenter()
        original.onSample(placeLat, placeLng, 0L)
        original.onSample(placeLat, placeLng, minute)
        original.onSample(37.545, placeLng, 2 * minute)
        original.onSample(37.5451, placeLng, 2 * minute + 30_000L)
        val traveling = LocationSegmentSnapshotCodec.decode(LocationSegmentSnapshotCodec.encode(original.snapshot()!!))

        // 복원 직후 마감: 이동 확정 시간 1분 30초 < 2분.
        assertTrue(segmenter(initialSnapshot = traveling).flush().isEmpty())

        val resumed = segmenter(initialSnapshot = traveling)
        resumed.onSample(37.56, placeLng, 4 * minute)
        val move = resumed.flush().single() as DetectedEvent.Move

        assertEquals(minute, move.startMillis)
        assertEquals(4 * minute, move.endMillis)
    }

    @Test
    fun `운영 기본 최소 이동 지속 시간은 20분이다`() {
        assertEquals(20 * minute, LocationSegmenter.DEFAULT_MIN_MOVEMENT_DURATION_MILLIS)
    }

    /** 체류 확정 후 [travelMillis]만큼 이동해 새 장소에 도착하는 시퀀스. 도착 시점의 이벤트를 돌려준다. */
    private fun LocationSegmenter.arriveAfterTravel(travelMillis: Long): List<DetectedEvent> {
        onSample(placeLat, placeLng, 0L)
        onSample(placeLat, placeLng, minute)
        val departure = minute + travelMillis
        onSample(37.54515, placeLng, departure)
        onSample(37.54515, placeLng, departure + 30_000L)
        return onSample(37.54515, placeLng, departure + 30_000L + minute)
    }

    @Test
    fun `flush는 확정 체류를 마감하고 스냅샷을 비운다`() {
        val segmenter = segmenter()
        segmenter.onSample(placeLat, placeLng, 0L)
        segmenter.onSample(placeLat, placeLng, 2 * minute)

        val dwell = segmenter.flush().single() as DetectedEvent.Dwell

        assertEquals(0L, dwell.startMillis)
        assertEquals(2 * minute, dwell.endMillis)
        assertEquals(null, segmenter.snapshot())
    }
}
