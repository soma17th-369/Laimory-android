package com.soma369.laimory.core.collection.location

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 프로세스 재시작 뒤에도 위치 분절을 이어가기 위한 순수 상태 스냅샷. */
@Serializable
internal data class LocationSegmentSnapshot(
    val state: LocationSegmentState,
    val previousSample: LocationSample,
)

@Serializable
internal sealed interface LocationSegmentState {
    @Serializable
    @SerialName("at_place")
    data class AtPlace(
        val place: PlaceAccumulator,
        val confirmed: Boolean,
        val consecutiveOutsideSamples: Int = 0,
        val firstOutsideSample: LocationSample? = null,
        val outsideDistanceMeters: Double = 0.0,
    ) : LocationSegmentState

    @Serializable
    @SerialName("traveling")
    data class Traveling(
        val rawId: String,
        val startLatitude: Double,
        val startLongitude: Double,
        val startMillis: Long,
        val totalDistanceMeters: Double,
        val candidateStartDistanceMeters: Double,
        val candidate: PlaceAccumulator,
        val consecutiveCandidateOutsideSamples: Int = 0,
        val firstCandidateOutsideSample: LocationSample? = null,
        val firstCandidateOutsideDistanceMeters: Double? = null,
    ) : LocationSegmentState
}

@Serializable
internal data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val timeMillis: Long,
    val accuracyMeters: Double,
)

/** 체류 중심점과 별개로 대표 좌표를 정확도 가중 평균으로 누적한다. */
@Serializable
internal data class PlaceAccumulator(
    val rawId: String,
    val anchorLatitude: Double,
    val anchorLongitude: Double,
    val sinceMillis: Long,
    val lastInsideSample: LocationSample,
    val weightedLatitudeSum: Double,
    val weightedLongitudeSum: Double,
    val weightSum: Double,
    val sampleCount: Int,
) {
    val latitude: Double
        get() = weightedLatitudeSum / weightSum

    val longitude: Double
        get() = weightedLongitudeSum / weightSum

    fun add(
        sample: LocationSample,
        minimumAccuracyMeters: Double,
    ): PlaceAccumulator {
        val weight = sample.weight(minimumAccuracyMeters)
        return copy(
            lastInsideSample = sample,
            weightedLatitudeSum = weightedLatitudeSum + sample.latitude * weight,
            weightedLongitudeSum = weightedLongitudeSum + sample.longitude * weight,
            weightSum = weightSum + weight,
            sampleCount = sampleCount + 1,
        )
    }

    companion object {
        fun from(
            rawId: String,
            sample: LocationSample,
            minimumAccuracyMeters: Double,
        ): PlaceAccumulator {
            val weight = sample.weight(minimumAccuracyMeters)
            return PlaceAccumulator(
                rawId = rawId,
                anchorLatitude = sample.latitude,
                anchorLongitude = sample.longitude,
                sinceMillis = sample.timeMillis,
                lastInsideSample = sample,
                weightedLatitudeSum = sample.latitude * weight,
                weightedLongitudeSum = sample.longitude * weight,
                weightSum = weight,
                sampleCount = 1,
            )
        }
    }
}

private fun LocationSample.weight(minimumAccuracyMeters: Double): Double {
    val boundedAccuracy = accuracyMeters.coerceAtLeast(minimumAccuracyMeters)
    return 1.0 / (boundedAccuracy * boundedAccuracy)
}
