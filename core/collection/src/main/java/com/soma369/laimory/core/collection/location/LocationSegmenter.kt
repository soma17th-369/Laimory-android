package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 위치 샘플 시퀀스를 체류/이동 이벤트로 분절한 결과. Android 의존이 없어 단위 테스트로 검증한다. */
internal sealed interface DetectedEvent {
    /** 한 장소(반경 내)에 머문 체류. 열린 체류는 같은 [rawId]로 반복 갱신된다. */
    data class Dwell(
        val rawId: String,
        val latitude: Double,
        val longitude: Double,
        val startMillis: Long,
        val endMillis: Long,
    ) : DetectedEvent

    /** 두 지점 사이 이동. [distanceMeters]는 샘플 경로 누적, [transport]는 평균 속도 추론이다. */
    data class Move(
        val rawId: String,
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
 * 위치 샘플을 체류([DetectedEvent.Dwell])와 이동([DetectedEvent.Move])으로 분절한다.
 *
 * - 반경 [dwellRadiusMeters] 안에서 [stayMillis] 이상 머물면 열린 체류를 확정하고 이후 샘플마다 갱신한다.
 * - 단발성 GPS 튐은 [requiredConsecutiveOutsideSamples] 연속 이탈 확인으로 흡수한다.
 * - [maxSampleGapMillis]보다 긴 공백은 이전 상태를 마지막 유효 샘플에서 닫고 새 구간을 시작한다.
 * - 이동은 실제 지속 시간이 [minMovementDurationMillis] 이상일 때만 확정한다 — 모든 마감 경로에 같은 기준을 적용한다.
 * - [snapshot]은 Room에 저장할 수 있는 순수 상태이며, [initialSnapshot]으로 프로세스 재시작 뒤 복원한다.
 */
internal class LocationSegmenter(
    private val dwellRadiusMeters: Double = DEFAULT_DWELL_RADIUS_METERS,
    private val stayMillis: Long = DEFAULT_STAY_MILLIS,
    private val maxSampleGapMillis: Long = DEFAULT_MAX_SAMPLE_GAP_MILLIS,
    private val maximumAccuracyMeters: Double = DEFAULT_MAXIMUM_ACCURACY_METERS,
    private val fallbackAccuracyMeters: Double = DEFAULT_FALLBACK_ACCURACY_METERS,
    private val minimumWeightAccuracyMeters: Double = DEFAULT_MINIMUM_WEIGHT_ACCURACY_METERS,
    private val requiredConsecutiveOutsideSamples: Int = DEFAULT_REQUIRED_CONSECUTIVE_OUTSIDE_SAMPLES,
    private val minMovementDurationMillis: Long = DEFAULT_MIN_MOVEMENT_DURATION_MILLIS,
    initialSnapshot: LocationSegmentSnapshot? = null,
    private val rawIdFactory: () -> String = { UUID.randomUUID().toString() },
) {
    private var currentSnapshot: LocationSegmentSnapshot? = initialSnapshot

    init {
        require(dwellRadiusMeters > 0.0)
        require(stayMillis > 0L)
        require(maxSampleGapMillis > 0L)
        require(maximumAccuracyMeters > 0.0)
        require(fallbackAccuracyMeters in 0.0..maximumAccuracyMeters)
        require(minimumWeightAccuracyMeters > 0.0)
        require(requiredConsecutiveOutsideSamples >= 1)
        require(minMovementDurationMillis >= 0L)
    }

    fun onSample(
        latitude: Double,
        longitude: Double,
        timeMillis: Long,
        accuracyMeters: Double? = null,
    ): List<DetectedEvent> {
        val sample = normalizeSample(latitude, longitude, timeMillis, accuracyMeters) ?: return emptyList()
        val snapshot = currentSnapshot
        if (snapshot != null && sample.timeMillis <= snapshot.previousSample.timeMillis) return emptyList()

        if (snapshot == null) {
            currentSnapshot = newAtPlaceSnapshot(sample)
            return emptyList()
        }

        if (sample.timeMillis - snapshot.previousSample.timeMillis > maxSampleGapMillis) {
            val events = closeCurrent(snapshot)
            currentSnapshot = newAtPlaceSnapshot(sample)
            return events
        }

        return when (val state = snapshot.state) {
            is LocationSegmentState.AtPlace -> handleAtPlace(snapshot, state, sample)
            is LocationSegmentState.Traveling -> handleTraveling(snapshot, state, sample)
        }
    }

    /** 현재 진행 상태를 영속 저장하기 위한 스냅샷. */
    fun snapshot(): LocationSegmentSnapshot? = currentSnapshot

    /** 추적 종료 시 열린 세그먼트를 마지막 유효 샘플에서 마감하고 진행 상태를 비운다. */
    fun flush(): List<DetectedEvent> {
        val snapshot = currentSnapshot ?: return emptyList()
        val events = closeCurrent(snapshot)
        currentSnapshot = null
        return events
    }

    /** 진행 중 세그먼트의 라이브 표시 상태. */
    fun currentStatus(nowMillis: Long): LocationTrackingStatus? =
        when (val state = currentSnapshot?.state) {
            is LocationSegmentState.AtPlace ->
                LocationTrackingStatus.Dwelling(
                    latitude = state.place.latitude,
                    longitude = state.place.longitude,
                    sinceMillis = state.place.sinceMillis,
                    nowMillis = nowMillis,
                )

            is LocationSegmentState.Traveling -> LocationTrackingStatus.Moving
            null -> null
        }

    private fun handleAtPlace(
        snapshot: LocationSegmentSnapshot,
        state: LocationSegmentState.AtPlace,
        sample: LocationSample,
    ): List<DetectedEvent> {
        val place = state.place
        val isInside =
            distanceMeters(place.anchorLatitude, place.anchorLongitude, sample.latitude, sample.longitude) < dwellRadiusMeters

        if (isInside) {
            val updatedPlace = place.add(sample, minimumWeightAccuracyMeters)
            val confirmed = state.confirmed || sample.timeMillis - place.sinceMillis >= stayMillis
            currentSnapshot =
                LocationSegmentSnapshot(
                    state = LocationSegmentState.AtPlace(place = updatedPlace, confirmed = confirmed),
                    previousSample = sample,
                )
            return if (confirmed) listOf(updatedPlace.toDwell()) else emptyList()
        }

        val outsideCount = state.consecutiveOutsideSamples + 1
        val outsideDistance =
            state.outsideDistanceMeters +
                if (state.consecutiveOutsideSamples == 0) {
                    distanceMeters(
                        place.lastInsideSample.latitude,
                        place.lastInsideSample.longitude,
                        sample.latitude,
                        sample.longitude,
                    )
                } else {
                    distanceMeters(
                        snapshot.previousSample.latitude,
                        snapshot.previousSample.longitude,
                        sample.latitude,
                        sample.longitude,
                    )
                }
        if (outsideCount < requiredConsecutiveOutsideSamples) {
            currentSnapshot =
                snapshot.copy(
                    state =
                        state.copy(
                            consecutiveOutsideSamples = outsideCount,
                            firstOutsideSample = state.firstOutsideSample ?: sample,
                            outsideDistanceMeters = outsideDistance,
                        ),
                    previousSample = sample,
                )
            return emptyList()
        }

        val firstOutside = state.firstOutsideSample ?: sample
        val events = mutableListOf<DetectedEvent>()
        if (state.confirmed || place.lastInsideSample.timeMillis - place.sinceMillis >= stayMillis) {
            events += place.toDwell()
        }

        val firstStepDistance =
            distanceMeters(
                place.lastInsideSample.latitude,
                place.lastInsideSample.longitude,
                firstOutside.latitude,
                firstOutside.longitude,
            )
        val candidate = newCandidateFromOutsideSamples(firstOutside, sample)
        currentSnapshot =
            LocationSegmentSnapshot(
                state =
                    LocationSegmentState.Traveling(
                        rawId = rawIdFactory(),
                        startLatitude = place.latitude,
                        startLongitude = place.longitude,
                        startMillis = place.lastInsideSample.timeMillis,
                        totalDistanceMeters = outsideDistance,
                        candidateStartDistanceMeters = firstStepDistance,
                        candidate = candidate,
                    ),
                previousSample = sample,
            )
        return events
    }

    private fun handleTraveling(
        snapshot: LocationSegmentSnapshot,
        state: LocationSegmentState.Traveling,
        sample: LocationSample,
    ): List<DetectedEvent> {
        val previous = snapshot.previousSample
        val totalDistance =
            state.totalDistanceMeters +
                distanceMeters(previous.latitude, previous.longitude, sample.latitude, sample.longitude)
        val candidate = state.candidate
        val isInsideCandidate =
            distanceMeters(
                candidate.anchorLatitude,
                candidate.anchorLongitude,
                sample.latitude,
                sample.longitude,
            ) < dwellRadiusMeters

        if (isInsideCandidate) {
            val updatedCandidate = candidate.add(sample, minimumWeightAccuracyMeters)
            if (sample.timeMillis - candidate.sinceMillis >= stayMillis) {
                val move = state.toMove(updatedCandidate)
                val dwell = updatedCandidate.toDwell()
                currentSnapshot =
                    LocationSegmentSnapshot(
                        state = LocationSegmentState.AtPlace(place = updatedCandidate, confirmed = true),
                        previousSample = sample,
                    )
                // 최소 지속 시간 미달로 이동이 걸러져도 도착한 체류는 그대로 확정한다.
                return listOfNotNull(move, dwell)
            }

            currentSnapshot =
                LocationSegmentSnapshot(
                    state =
                        state.copy(
                            totalDistanceMeters = totalDistance,
                            candidate = updatedCandidate,
                            consecutiveCandidateOutsideSamples = 0,
                            firstCandidateOutsideSample = null,
                            firstCandidateOutsideDistanceMeters = null,
                        ),
                    previousSample = sample,
                )
            return emptyList()
        }

        val outsideCount = state.consecutiveCandidateOutsideSamples + 1
        if (outsideCount < requiredConsecutiveOutsideSamples) {
            currentSnapshot =
                LocationSegmentSnapshot(
                    state =
                        state.copy(
                            totalDistanceMeters = totalDistance,
                            consecutiveCandidateOutsideSamples = outsideCount,
                            firstCandidateOutsideSample = state.firstCandidateOutsideSample ?: sample,
                            firstCandidateOutsideDistanceMeters =
                                state.firstCandidateOutsideDistanceMeters ?: totalDistance,
                        ),
                    previousSample = sample,
                )
            return emptyList()
        }

        val firstOutside = state.firstCandidateOutsideSample ?: sample
        val candidateStartDistance =
            state.firstCandidateOutsideDistanceMeters ?: totalDistance
        currentSnapshot =
            LocationSegmentSnapshot(
                state =
                    state.copy(
                        totalDistanceMeters = totalDistance,
                        candidateStartDistanceMeters = candidateStartDistance,
                        candidate = newCandidateFromOutsideSamples(firstOutside, sample),
                        consecutiveCandidateOutsideSamples = 0,
                        firstCandidateOutsideSample = null,
                        firstCandidateOutsideDistanceMeters = null,
                    ),
                previousSample = sample,
            )
        return emptyList()
    }

    private fun closeCurrent(snapshot: LocationSegmentSnapshot): List<DetectedEvent> =
        when (val state = snapshot.state) {
            is LocationSegmentState.AtPlace -> {
                val place = state.place
                if (state.confirmed || place.lastInsideSample.timeMillis - place.sinceMillis >= stayMillis) {
                    listOf(place.toDwell())
                } else {
                    emptyList()
                }
            }

            is LocationSegmentState.Traveling ->
                // 지속 시간 판정이 시각 역전(끝 <= 시작)까지 함께 걸러낸다.
                listOfNotNull(state.toMove(snapshot.previousSample))
        }

    private fun newAtPlaceSnapshot(sample: LocationSample): LocationSegmentSnapshot =
        LocationSegmentSnapshot(
            state =
                LocationSegmentState.AtPlace(
                    place = PlaceAccumulator.from(rawIdFactory(), sample, minimumWeightAccuracyMeters),
                    confirmed = false,
                ),
            previousSample = sample,
        )

    private fun newCandidateFromOutsideSamples(
        firstOutside: LocationSample,
        current: LocationSample,
    ): PlaceAccumulator {
        val candidateRawId = rawIdFactory()
        val first = PlaceAccumulator.from(candidateRawId, firstOutside, minimumWeightAccuracyMeters)
        return if (
            firstOutside.timeMillis != current.timeMillis &&
            distanceMeters(firstOutside.latitude, firstOutside.longitude, current.latitude, current.longitude) < dwellRadiusMeters
        ) {
            first.add(current, minimumWeightAccuracyMeters)
        } else {
            PlaceAccumulator.from(candidateRawId, current, minimumWeightAccuracyMeters)
        }
    }

    private fun normalizeSample(
        latitude: Double,
        longitude: Double,
        timeMillis: Long,
        accuracyMeters: Double?,
    ): LocationSample? {
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
        if (timeMillis < 0L) return null
        val accuracy =
            accuracyMeters
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?: fallbackAccuracyMeters
        if (accuracy > maximumAccuracyMeters) return null
        return LocationSample(latitude, longitude, timeMillis, accuracy)
    }

    private fun PlaceAccumulator.toDwell(): DetectedEvent.Dwell =
        DetectedEvent.Dwell(
            rawId = rawId,
            latitude = latitude,
            longitude = longitude,
            startMillis = sinceMillis,
            endMillis = lastInsideSample.timeMillis,
        )

    private fun LocationSegmentState.Traveling.toMove(endPlace: PlaceAccumulator): DetectedEvent.Move? =
        toMove(
            endLatitude = endPlace.latitude,
            endLongitude = endPlace.longitude,
            endMillis = endPlace.sinceMillis,
            eventDistanceMeters = candidateStartDistanceMeters,
        )

    private fun LocationSegmentState.Traveling.toMove(end: LocationSample): DetectedEvent.Move? =
        toMove(
            endLatitude = end.latitude,
            endLongitude = end.longitude,
            endMillis = end.timeMillis,
            eventDistanceMeters = totalDistanceMeters,
        )

    /**
     * 모든 마감 경로가 거치는 단일 확정 지점.
     *
     * 실제 이동 시간([endMillis] - startMillis)이 [minMovementDurationMillis] 미만이면 확정하지 않는다.
     * 이동수단 분류·거리 계산보다 앞서 판정하므로 경로별로 기준이 갈리지 않는다.
     */
    private fun LocationSegmentState.Traveling.toMove(
        endLatitude: Double,
        endLongitude: Double,
        endMillis: Long,
        eventDistanceMeters: Double,
    ): DetectedEvent.Move? {
        if (endMillis - startMillis < minMovementDurationMillis) return null
        return DetectedEvent.Move(
            rawId = rawId,
            startLatitude = startLatitude,
            startLongitude = startLongitude,
            endLatitude = endLatitude,
            endLongitude = endLongitude,
            startMillis = startMillis,
            endMillis = endMillis,
            distanceMeters = eventDistanceMeters,
            transport = inferTransport(eventDistanceMeters, endMillis - startMillis),
        )
    }

    private fun inferTransport(
        distanceMeters: Double,
        durationMillis: Long,
    ): MovementPayload.Transport = MovementTransportClassifier.fromAverageSpeed(distanceMeters, durationMillis)

    companion object {
        /** 초기 운영 체류 반경. */
        const val DEFAULT_DWELL_RADIUS_METERS = 300.0

        /** 초기 운영 체류 인정 시간. */
        const val DEFAULT_STAY_MILLIS = 20 * 60_000L

        /** 마지막 저장 샘플과 복원 후 첫 샘플을 이어 붙일 수 있는 최대 공백. */
        const val DEFAULT_MAX_SAMPLE_GAP_MILLIS = 5 * 60_000L

        /**
         * 판정과 대표 좌표 계산에 사용할 수 있는 최대 수평 정확도.
         * 실내·지하에서는 모든 샘플이 제외되어 체류가 기록되지 않을 수 있으므로, 수집 공백이 관찰되면 우선 조정한다.
         */
        const val DEFAULT_MAXIMUM_ACCURACY_METERS = 100.0

        /** 정확도 정보가 없는 샘플에 적용하는 보수적인 정확도. */
        const val DEFAULT_FALLBACK_ACCURACY_METERS = 100.0

        /** 비정상적으로 작은 정확도 값 하나가 대표 좌표를 독점하지 않도록 하는 하한. */
        const val DEFAULT_MINIMUM_WEIGHT_ACCURACY_METERS = 5.0

        /** 단발성 GPS 튐을 흡수하기 위한 연속 반경 이탈 샘플 수. */
        const val DEFAULT_REQUIRED_CONSECUTIVE_OUTSIDE_SAMPLES = 2

        /**
         * 확정 저장할 이동의 최소 지속 시간.
         *
         * 관측 가능한 연속 구간 기준이다 — 샘플 공백으로 분절된 조각은 각각 판정하므로,
         * 합산이 이 값을 넘어도 개별 조각이 미달이면 저장하지 않는다.
         */
        const val DEFAULT_MIN_MOVEMENT_DURATION_MILLIS = 20 * 60_000L

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
