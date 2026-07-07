package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.MovementPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSegmenterTest {
    // 테스트용: 체류 판정 1분.
    private fun segmenter() = LocationSegmenter(dwellRadiusMeters = 80.0, stayMillis = 60_000L)

    private val minute = 60_000L
    private val placeLat = 37.5000
    private val placeLng = 127.0000

    @Test
    fun `haversine 거리 계산이 대략 맞다`() {
        // 위도 0.045도 차이 ~= 약 5km.
        val d = LocationSegmenter.distanceMeters(37.5, 127.0, 37.545, 127.0)
        assertTrue("expected ~5000m but was $d", d in 4800.0..5200.0)
    }

    @Test
    fun `한 장소에 충분히 머문 뒤 떠나면 체류(Dwell) 이벤트를 낸다`() {
        val s = segmenter()
        val events = mutableListOf<DetectedEvent>()
        // 0~2분 같은 장소.
        events += s.onSample(placeLat, placeLng, 0)
        events += s.onSample(placeLat, placeLng, minute)
        events += s.onSample(placeLat, placeLng, 2 * minute)
        // 멀리 이동(약 5km 북쪽) → 체류 마감.
        events += s.onSample(37.545, 127.0, 3 * minute)

        val dwell = events.filterIsInstance<DetectedEvent.Dwell>().single()
        assertEquals(placeLat, dwell.latitude, 1e-9)
        assertEquals(placeLng, dwell.longitude, 1e-9)
        assertEquals(0L, dwell.startMillis)
        assertEquals(2 * minute, dwell.endMillis)
    }

    @Test
    fun `체류→빠른 이동→체류 시퀀스는 IN_VEHICLE 이동을 낸다`() {
        val s = segmenter()
        val events = mutableListOf<DetectedEvent>()
        // 장소 P 체류.
        events += s.onSample(placeLat, placeLng, 0)
        events += s.onSample(placeLat, placeLng, minute)
        events += s.onSample(placeLat, placeLng, 2 * minute)
        // 5km 떨어진 Q 로 빠르게 이동(1분).
        val qLat = 37.54515
        val qLng = 127.0
        events += s.onSample(qLat, qLng, 3 * minute)
        // Q 에 안착(체류 판정 시간 이상).
        events += s.onSample(qLat, qLng, 4 * minute)

        val move = events.filterIsInstance<DetectedEvent.Move>().single()
        assertEquals(placeLat, move.startLatitude, 1e-9)
        assertEquals(qLat, move.endLatitude, 1e-9)
        // 5km/1분 ~= 83 m/s → 차량.
        assertEquals(MovementPayload.Transport.IN_VEHICLE, move.transport)
        assertTrue("distance ~5km", move.distanceMeters in 4800.0..5200.0)
    }

    @Test
    fun `flush 는 마감 안 된 체류를 낸다`() {
        val s = segmenter()
        s.onSample(placeLat, placeLng, 0)
        s.onSample(placeLat, placeLng, 2 * minute)
        val flushed = s.flush()
        val dwell = flushed.filterIsInstance<DetectedEvent.Dwell>().single()
        assertEquals(0L, dwell.startMillis)
        assertEquals(2 * minute, dwell.endMillis)
    }
}
