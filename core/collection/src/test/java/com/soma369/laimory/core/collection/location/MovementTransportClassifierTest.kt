package com.soma369.laimory.core.collection.location

import com.google.android.gms.location.DetectedActivity
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class MovementTransportClassifierTest {
    @Test
    fun `도보 계열 활동은 WALKING으로 정규화한다`() {
        listOf(DetectedActivity.WALKING, DetectedActivity.RUNNING, DetectedActivity.ON_FOOT).forEach { activity ->
            assertEquals(
                MovementPayload.Transport.WALKING,
                MovementTransportClassifier.fromDetectedActivity(activity),
            )
        }
    }

    @Test
    fun `차량 활동은 IN_VEHICLE로 정규화한다`() {
        assertEquals(
            MovementPayload.Transport.IN_VEHICLE,
            MovementTransportClassifier.fromDetectedActivity(DetectedActivity.IN_VEHICLE),
        )
    }

    @Test
    fun `자전거와 정지 및 알 수 없는 활동은 속도 폴백을 위해 UNKNOWN으로 둔다`() {
        listOf(DetectedActivity.ON_BICYCLE, DetectedActivity.STILL, DetectedActivity.UNKNOWN).forEach { activity ->
            assertEquals(
                MovementPayload.Transport.UNKNOWN,
                MovementTransportClassifier.fromDetectedActivity(activity),
            )
        }
    }

    @Test
    fun `평균 속도 4mps 미만은 도보이고 경계부터 차량이다`() {
        assertEquals(
            MovementPayload.Transport.WALKING,
            MovementTransportClassifier.fromAverageSpeed(distanceMeters = 3.999, durationMillis = 1_000L),
        )
        assertEquals(
            MovementPayload.Transport.IN_VEHICLE,
            MovementTransportClassifier.fromAverageSpeed(distanceMeters = 4.0, durationMillis = 1_000L),
        )
    }

    @Test
    fun `시간이 유효하지 않은 확정 이동도 UNKNOWN으로 저장하지 않는다`() {
        assertEquals(
            MovementPayload.Transport.WALKING,
            MovementTransportClassifier.fromAverageSpeed(distanceMeters = 100.0, durationMillis = 0L),
        )
    }
}
