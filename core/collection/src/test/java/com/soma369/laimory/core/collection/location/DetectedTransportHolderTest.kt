package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.MovementPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectedTransportHolderTest {
    @Test
    fun `UNKNOWN 점유 시간은 다른 이동수단에 귀속하지 않는다`() {
        val holder = DetectedTransportHolder()
        holder.onEnter(MovementPayload.Transport.WALKING, atMillis = 0L)
        holder.onEnter(MovementPayload.Transport.UNKNOWN, atMillis = 10L)

        assertEquals(MovementPayload.Transport.WALKING, holder.dominant(nowMillis = 20L))
    }

    @Test
    fun `체류에서 이동으로 전환되면 체류 중 감지값을 초기화한다`() {
        val segmenter = LocationSegmenter(dwellRadiusMeters = 80.0, stayMillis = 60_000L)
        val holder = DetectedTransportHolder()
        segmenter.onSample(latitude = 37.5, longitude = 127.0, timeMillis = 0L)
        val previousStatus = segmenter.currentStatus(nowMillis = 0L)
        holder.onEnter(MovementPayload.Transport.IN_VEHICLE, atMillis = 0L)

        segmenter.onSample(latitude = 37.501, longitude = 127.0, timeMillis = 30_000L)
        val currentStatus = segmenter.currentStatus(nowMillis = 30_000L)
        holder.onTrackingStatusChanged(previousStatus, currentStatus)

        assertEquals(MovementPayload.Transport.UNKNOWN, holder.dominant(nowMillis = 30_000L))
    }
}
