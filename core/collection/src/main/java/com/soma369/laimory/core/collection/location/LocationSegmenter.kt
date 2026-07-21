package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 위치 샘플 시퀀스를 체류/이동 이벤트로 분절한 결과. Android 의존이 없어 단위 테스트로 검증한다. */
internal sealed interface DetectedEvent {
    /** 한 장소(반경 내)에 머문 체류. 체류 시간은 start/end 에서 파생한다. */
    data class Dwell(
        val latitude: Double,
        val longitude: Double,
        val startMillis: Long,
        val endMillis: Long,
    ) : DetectedEvent

    /** 두 지점 사이 이동. [distanceMeters] 는 샘플 경로 누적, [transport] 는 평균 속도 추론. */
    data class Move(
        val startLatitude: Double,
        val startLongitude: Double,
        val endLatitude: Double,
        val endLongitude: Double,
        val startMillis: Long,
        val endMillis: Long,
        val distanceMeters: Double,
        val transport: MovementPayload.Transport,
    ) : DetectedEvent
}

/**
 * 위치 샘플을 받아 체류([DetectedEvent.Dwell])와 이동([DetectedEvent.Move])으로 분절한다.
 *
 * 판정 규칙(MVP): 반경 [dwellRadiusMeters] 안에 [stayMillis] 이상 머물면 한 장소(체류)로 보고, 반경을 벗어나
 * 다른 장소에 안착할 때까지를 이동으로 본다. 이동수단은 평균 속도로 추론한다. 순수 로직이므로 상태를 내부에 들고,
 * [onSample] 이 세그먼트가 닫힐 때 이벤트를 반환하며, [flush] 로 열린 세그먼트를 마감한다.
 */
internal class LocationSegmenter(
    private val dwellRadiusMeters: Double = DEFAULT_DWELL_RADIUS_METERS,
    private val stayMillis: Long = DEFAULT_STAY_MILLIS,
) {
    private var atPlace: AtPlace? = null
    private var traveling: Traveling? = null

    private var prevLatitude = 0.0
    private var prevLongitude = 0.0
    private var prevMillis = 0L
    private var hasPrev = false

    fun onSample(
        latitude: Double,
        longitude: Double,
        timeMillis: Long,
    ): List<DetectedEvent> {
        val events = mutableListOf<DetectedEvent>()
        val place = atPlace
        val trip = traveling
        when {
            place == null && trip == null -> atPlace = AtPlace(latitude, longitude, timeMillis)

            place != null -> {
                if (distanceMeters(place.latitude, place.longitude, latitude, longitude) >= dwellRadiusMeters) {
                    // 장소를 벗어남 → 체류 마감(충분히 머물렀으면) 후 이동 시작.
                    if (hasPrev && prevMillis - place.since >= stayMillis) {
                        events += DetectedEvent.Dwell(place.latitude, place.longitude, place.since, prevMillis)
                    }
                    val startMillis = if (hasPrev) prevMillis else place.since
                    val stepDistance = if (hasPrev) distanceMeters(prevLatitude, prevLongitude, latitude, longitude) else 0.0
                    atPlace = null
                    traveling =
                        Traveling(
                            startLatitude = place.latitude,
                            startLongitude = place.longitude,
                            startMillis = startMillis,
                            distanceMeters = stepDistance,
                            candidateLatitude = latitude,
                            candidateLongitude = longitude,
                            candidateSince = timeMillis,
                        )
                }
            }

            trip != null -> {
                if (hasPrev) trip.distanceMeters += distanceMeters(prevLatitude, prevLongitude, latitude, longitude)
                if (distanceMeters(trip.candidateLatitude, trip.candidateLongitude, latitude, longitude) >= dwellRadiusMeters) {
                    // 아직 이동 중 — 안착 후보를 현재 위치로 갱신.
                    trip.candidateLatitude = latitude
                    trip.candidateLongitude = longitude
                    trip.candidateSince = timeMillis
                } else if (timeMillis - trip.candidateSince >= stayMillis) {
                    // 후보 지점에 충분히 머무름 → 도착(이동 마감).
                    events +=
                        DetectedEvent.Move(
                            startLatitude = trip.startLatitude,
                            startLongitude = trip.startLongitude,
                            endLatitude = trip.candidateLatitude,
                            endLongitude = trip.candidateLongitude,
                            startMillis = trip.startMillis,
                            endMillis = trip.candidateSince,
                            distanceMeters = trip.distanceMeters,
                            transport = inferTransport(trip.distanceMeters, trip.candidateSince - trip.startMillis),
                        )
                    traveling = null
                    atPlace = AtPlace(trip.candidateLatitude, trip.candidateLongitude, trip.candidateSince)
                }
            }
        }
        prevLatitude = latitude
        prevLongitude = longitude
        prevMillis = timeMillis
        hasPrev = true
        return events
    }

    /** 추적 종료 시 열린 세그먼트를 마감한다. */
    fun flush(): List<DetectedEvent> {
        if (!hasPrev) return emptyList()
        val place = atPlace
        val trip = traveling
        atPlace = null
        traveling = null
        return when {
            place != null && prevMillis - place.since >= stayMillis ->
                listOf(DetectedEvent.Dwell(place.latitude, place.longitude, place.since, prevMillis))

            trip != null ->
                listOf(
                    DetectedEvent.Move(
                        startLatitude = trip.startLatitude,
                        startLongitude = trip.startLongitude,
                        endLatitude = prevLatitude,
                        endLongitude = prevLongitude,
                        startMillis = trip.startMillis,
                        endMillis = prevMillis,
                        distanceMeters = trip.distanceMeters,
                        transport = inferTransport(trip.distanceMeters, prevMillis - trip.startMillis),
                    ),
                )

            else -> emptyList()
        }
    }

    /** 진행 중인 세그먼트의 현재 상태(라이브 표시용). 장소에 머무는 중이면 Dwelling, 이동 중이면 Moving, 없으면 null. */
    fun currentStatus(nowMillis: Long): LocationTrackingStatus? {
        val place = atPlace
        return when {
            place != null -> LocationTrackingStatus.Dwelling(place.latitude, place.longitude, place.since, nowMillis)
            traveling != null -> LocationTrackingStatus.Moving
            else -> null
        }
    }

    private fun inferTransport(
        distanceMeters: Double,
        durationMillis: Long,
    ): MovementPayload.Transport = MovementTransportClassifier.fromAverageSpeed(distanceMeters, durationMillis)

    private data class AtPlace(
        val latitude: Double,
        val longitude: Double,
        val since: Long,
    )

    private class Traveling(
        val startLatitude: Double,
        val startLongitude: Double,
        val startMillis: Long,
        var distanceMeters: Double,
        var candidateLatitude: Double,
        var candidateLongitude: Double,
        var candidateSince: Long,
    )

    companion object {
        /**
         * 운영 기본 체류 반경(100m). GPS 오차와 도심 흔들림을 흡수하되, 장소 단위 체류를 권역 단위로
         * 너무 넓게 합치지 않기 위한 MVP 기준.
         */
        const val DEFAULT_DWELL_RADIUS_METERS = 100.0

        /** 운영 기본 체류 인정 시간(5분). 교통 정차 등 짧은 멈춤은 걸러 의미있는 방문만 남긴다. */
        const val DEFAULT_STAY_MILLIS = 5 * 60_000L

        /** 두 좌표 사이 대권거리(Haversine, m). */
        fun distanceMeters(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double,
        ): Double {
            val earthRadius = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a =
                sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
            return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
