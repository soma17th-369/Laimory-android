package com.soma369.laimory.core.data.model.timeline.request

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.LocationPayload
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.ZoneId

/**
 * 수집 [SourceItem] 을 초안 요청의 [SourceItemDto] 로 투영한다.
 *
 * 서버 payload 계약은 수집 저장 포맷과 다르므로(HEALTH 표시문자열, NOTIFICATION packageName 제외,
 * PHOTO filename=업로드 결과 등) 여기서 타입별로 맞춘다. `startAt`/`endAt` 은 아이템의 시간대 기준
 * 오프셋 없는 로컬 datetime 으로 렌더한다.
 *
 * @param uploadedPhotoFilename PHOTO 일 때 업로드로 받은 서버 파일명. 없으면 발급/매핑 누락이라 실패시킨다.
 */
internal fun SourceItem.toSourceItemDto(
    json: Json,
    uploadedPhotoFilename: String?,
): SourceItemDto =
    SourceItemDto(
        itemType = itemType.name,
        rawId = rawId,
        startAt = localDateTime(startAt, timeZoneId),
        endAt = endAt?.let { localDateTime(it, timeZoneId) },
        payload = payload.toPayloadJson(json, uploadedPhotoFilename),
    )

private fun SourceItemPayload.toPayloadJson(
    json: Json,
    uploadedPhotoFilename: String?,
): JsonObject =
    when (this) {
        is PhotoPayload ->
            json.encodeToJsonElement(
                DraftPhotoPayloadDto(
                    filename =
                        uploadedPhotoFilename
                            ?: throw ApiException.UnknownException("PHOTO 업로드 파일명이 없습니다: $clientPhotoUri"),
                    clientPhotoUri = clientPhotoUri,
                    latitude = latitude,
                    longitude = longitude,
                    description = description,
                ),
            )
        is CalendarPayload ->
            json.encodeToJsonElement(
                DraftCalendarPayloadDto(
                    title = title,
                    locationText = locationText,
                    description = description,
                    allDay = allDay,
                ),
            )
        is LocationPayload ->
            json.encodeToJsonElement(DraftLocationPayloadDto(latitude = latitude, longitude = longitude))
        is MovementPayload ->
            json.encodeToJsonElement(
                DraftMovementPayloadDto(
                    start = start.toDto(),
                    end = end.toDto(),
                    transports = transports.name,
                    distanceMeters = distanceMeters,
                ),
            )
        is HealthPayload ->
            json.encodeToJsonElement(DraftHealthPayloadDto(metric = metric.name, value = healthDisplayValue(this)))
        is NotificationPayload ->
            json.encodeToJsonElement(DraftNotificationPayloadDto(appName = appName, title = title, text = text))
    }.jsonObject

/** 걸음수·수면은 표시 문자열로 보낸다: STEPS "8421보" / SLEEP "480분". */
private fun healthDisplayValue(payload: HealthPayload): String {
    val amount = payload.value.toInt()
    return when (payload.metric) {
        HealthPayload.Metric.STEPS -> "${amount}보"
        HealthPayload.Metric.SLEEP -> "${amount}분"
    }
}

/** [instant] 를 [zone] 기준 로컬 datetime 문자열(오프셋 없음)로 렌더한다. */
private fun localDateTime(
    instant: Instant,
    zone: ZoneId,
): String = instant.atZone(zone).toLocalDateTime().toString()

private fun GeoPoint.toDto() = DraftGeoPointDto(latitude = latitude, longitude = longitude)

@Serializable
private data class DraftPhotoPayloadDto(
    val filename: String,
    val clientPhotoUri: String,
    val latitude: Double?,
    val longitude: Double?,
    val description: String?,
)

@Serializable
private data class DraftCalendarPayloadDto(
    val title: String,
    val locationText: String?,
    val description: String?,
    val allDay: Boolean,
)

@Serializable
private data class DraftLocationPayloadDto(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
private data class DraftGeoPointDto(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
private data class DraftMovementPayloadDto(
    val start: DraftGeoPointDto,
    val end: DraftGeoPointDto,
    val transports: String,
    val distanceMeters: Double,
)

@Serializable
private data class DraftHealthPayloadDto(
    val metric: String,
    val value: String,
)

@Serializable
private data class DraftNotificationPayloadDto(
    val appName: String,
    val title: String?,
    val text: String?,
)
