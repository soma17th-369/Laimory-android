package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.MovementPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectedTransportHolderTest {
    @Test
    fun `과거 달리기는 도보로 정규화하고 자전거는 속도 폴백으로 보낸다`() {
        val holder = DetectedTransportHolder()
        holder.onEnter(MovementPayload.Transport.RUNNING, atMillis = 0L)
        assertEquals(MovementPayload.Transport.WALKING, holder.dominant(nowMillis = 10L))

        holder.reset()
        holder.onEnter(MovementPayload.Transport.ON_BICYCLE, atMillis = 0L)
        assertEquals(MovementPayload.Transport.UNKNOWN, holder.dominant(nowMillis = 10L))
    }

    @Test
    fun `이동 시작 경계에서 초기화하면 체류 중 감지값이 남지 않는다`() {
        val holder = DetectedTransportHolder()
        holder.onEnter(MovementPayload.Transport.ON_BICYCLE, atMillis = 0L)

        holder.reset()

        assertEquals(MovementPayload.Transport.UNKNOWN, holder.dominant(nowMillis = 20L))
    }
}
