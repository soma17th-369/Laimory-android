package com.soma369.laimory.core.collection.location

import com.google.android.gms.location.DetectedActivity
import com.soma369.laimory.core.domain.model.collection.MovementPayload

/** 신규 이동 기록의 이동수단을 도보/차량 두 값으로 정규화하는 단일 정책 지점. */
internal object MovementTransportClassifier {
    private const val VEHICLE_MIN_MPS = 4.0

    fun fromDetectedActivity(activityType: Int): MovementPayload.Transport =
        when (activityType) {
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_FOOT,
            -> MovementPayload.Transport.WALKING

            DetectedActivity.IN_VEHICLE -> MovementPayload.Transport.IN_VEHICLE

            else -> MovementPayload.Transport.UNKNOWN
        }

    /** 위치로 이동이 확정된 구간이므로 유효 시간이 없더라도 UNKNOWN 대신 보수적으로 도보 처리한다. */
    fun fromAverageSpeed(
        distanceMeters: Double,
        durationMillis: Long,
    ): MovementPayload.Transport {
        if (durationMillis <= 0L) return MovementPayload.Transport.WALKING
        val speedMetersPerSecond = distanceMeters.coerceAtLeast(0.0) / (durationMillis / 1000.0)
        return if (speedMetersPerSecond < VEHICLE_MIN_MPS) {
            MovementPayload.Transport.WALKING
        } else {
            MovementPayload.Transport.IN_VEHICLE
        }
    }

    /** 과거 enum 값이 신규 수집 경로로 들어와도 저장 전 도보/차량으로 제한한다. */
    fun normalize(transport: MovementPayload.Transport): MovementPayload.Transport =
        when (transport) {
            MovementPayload.Transport.WALKING,
            MovementPayload.Transport.RUNNING,
            -> MovementPayload.Transport.WALKING

            MovementPayload.Transport.IN_VEHICLE -> MovementPayload.Transport.IN_VEHICLE

            MovementPayload.Transport.ON_BICYCLE,
            MovementPayload.Transport.UNKNOWN,
            -> MovementPayload.Transport.UNKNOWN
        }
}
