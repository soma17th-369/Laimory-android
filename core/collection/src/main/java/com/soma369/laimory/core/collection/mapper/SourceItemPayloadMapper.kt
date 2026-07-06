package com.soma369.laimory.core.collection.mapper

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.LocationPayload
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 카테고리별 payload 를 로컬 저장용 JSON 문자열로 직렬화/역직렬화한다.
 *
 * JSON 필드명은 서버 업로드 포맷의 payload 필드명을 그대로 따라간다 —
 * 업로드 조립 시 payload 는 변환 없이 사용할 수 있게 한다.
 */
internal object SourceItemPayloadMapper {
    private val json =
        Json {
            // 이후 payload 에 필드가 추가되어도 기존 저장분을 읽을 수 있게 한다
            ignoreUnknownKeys = true
        }

    fun toJson(payload: SourceItemPayload): String =
        when (payload) {
            is LocationPayload -> json.encodeToString(payload.toDto())
            is MovementPayload -> json.encodeToString(payload.toDto())
            is CalendarPayload -> json.encodeToString(payload.toDto())
            is HealthPayload -> json.encodeToString(payload.toDto())
            is NotificationPayload -> json.encodeToString(payload.toDto())
            is PhotoPayload -> json.encodeToString(payload.toDto())
        }

    fun fromJson(
        itemType: ItemType,
        payloadJson: String,
    ): SourceItemPayload =
        when (itemType) {
            ItemType.LOCATION -> json.decodeFromString<LocationPayloadDto>(payloadJson).toDomain()
            ItemType.MOVEMENT -> json.decodeFromString<MovementPayloadDto>(payloadJson).toDomain()
            ItemType.CALENDAR -> json.decodeFromString<CalendarPayloadDto>(payloadJson).toDomain()
            ItemType.HEALTH -> json.decodeFromString<HealthPayloadDto>(payloadJson).toDomain()
            ItemType.NOTIFICATION -> json.decodeFromString<NotificationPayloadDto>(payloadJson).toDomain()
            ItemType.PHOTO -> json.decodeFromString<PhotoPayloadDto>(payloadJson).toDomain()
        }
}

@Serializable
internal data class GeoPointDto(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
internal data class LocationPayloadDto(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
internal data class MovementPayloadDto(
    val start: GeoPointDto,
    val end: GeoPointDto,
    val distanceMeters: Double,
    val transport: String,
)

@Serializable
internal data class CalendarPayloadDto(
    val title: String,
    val description: String?,
    val locationText: String?,
    val allDay: Boolean,
)

@Serializable
internal data class HealthPayloadDto(
    val metric: String,
    val value: Double,
    val unit: String,
)

@Serializable
internal data class NotificationPayloadDto(
    val appName: String,
    val title: String?,
    val text: String?,
)

@Serializable
internal data class PhotoPayloadDto(
    val filename: String,
    val clientPhotoUri: String,
    val latitude: Double?,
    val longitude: Double?,
    val description: String?,
)

private fun GeoPoint.toDto() = GeoPointDto(latitude = latitude, longitude = longitude)

private fun GeoPointDto.toDomain() = GeoPoint(latitude = latitude, longitude = longitude)

private fun LocationPayload.toDto() = LocationPayloadDto(latitude = latitude, longitude = longitude)

private fun LocationPayloadDto.toDomain() = LocationPayload(latitude = latitude, longitude = longitude)

private fun MovementPayload.toDto() =
    MovementPayloadDto(
        start = start.toDto(),
        end = end.toDto(),
        distanceMeters = distanceMeters,
        transport = transport.name,
    )

private fun MovementPayloadDto.toDomain() =
    MovementPayload(
        start = start.toDomain(),
        end = end.toDomain(),
        distanceMeters = distanceMeters,
        transport = MovementPayload.Transport.valueOf(transport),
    )

private fun CalendarPayload.toDto() =
    CalendarPayloadDto(
        title = title,
        description = description,
        locationText = locationText,
        allDay = allDay,
    )

private fun CalendarPayloadDto.toDomain() =
    CalendarPayload(
        title = title,
        description = description,
        locationText = locationText,
        allDay = allDay,
    )

private fun HealthPayload.toDto() =
    HealthPayloadDto(
        metric = metric.name,
        value = value,
        unit = unit,
    )

private fun HealthPayloadDto.toDomain() =
    HealthPayload(
        metric = HealthPayload.Metric.valueOf(metric),
        value = value,
        unit = unit,
    )

private fun NotificationPayload.toDto() =
    NotificationPayloadDto(
        appName = appName,
        title = title,
        text = text,
    )

private fun NotificationPayloadDto.toDomain() =
    NotificationPayload(
        appName = appName,
        title = title,
        text = text,
    )

private fun PhotoPayload.toDto() =
    PhotoPayloadDto(
        filename = fileName,
        clientPhotoUri = clientPhotoUri,
        latitude = latitude,
        longitude = longitude,
        description = description,
    )

private fun PhotoPayloadDto.toDomain() =
    PhotoPayload(
        fileName = filename,
        clientPhotoUri = clientPhotoUri,
        latitude = latitude,
        longitude = longitude,
        description = description,
    )
