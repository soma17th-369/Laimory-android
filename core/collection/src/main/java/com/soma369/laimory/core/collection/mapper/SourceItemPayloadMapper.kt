package com.soma369.laimory.core.collection.mapper

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.StayPayload
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 카테고리별 payload 를 로컬 저장용 JSON 문자열로 직렬화/역직렬화한다.
 *
 * JSON 필드명은 서버 업로드 포맷의 payload 필드명을 따라간다. 다만 업로드 envelope 는
 * 저장 원본이 아니라 projection 결과이므로, 업로드 조립 시 일부 필드는
 * 렌더링되거나(HEALTH 의 value+unit → "xx보") 제외될 수 있다(NOTIFICATION 의 packageName 등).
 */
internal object SourceItemPayloadMapper {
    private val json =
        Json {
            // 이후 payload 에 필드가 추가되어도 기존 저장분을 읽을 수 있게 한다
            ignoreUnknownKeys = true
        }

    fun toJson(payload: SourceItemPayload): String =
        when (payload) {
            is StayPayload -> json.encodeToString(payload.toDto())
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
            ItemType.STAY -> json.decodeFromString<StayPayloadDto>(payloadJson).toDomain()
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
    // MOVEMENT 출발·도착 위치의 로컬 화면 표시 전용 주소.
    val address: String? = null,
)

@Serializable
internal data class StayPayloadDto(
    val latitude: Double,
    val longitude: Double,
    // 로컬 화면 표시 전용. 기존 저장분에는 없으므로 null 기본값으로 호환한다.
    val address: String? = null,
)

@Serializable
internal data class MovementPayloadDto(
    val start: GeoPointDto,
    val end: GeoPointDto,
    // 기존 저장분/누락 시 기본값으로 읽는다(ignoreUnknownKeys 와 함께 안전한 진화).
    val distanceMeters: Double = 0.0,
    val transports: String = "UNKNOWN",
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
    val packageName: String,
    val title: String?,
    val text: String?,
    // 로컬 저장 전용. 기존 저장분/누락 시 ALL 로 읽는다(ignoreUnknownKeys 와 함께 안전한 진화).
    val collectReason: String = NotificationPayload.CollectReason.ALL.name,
)

@Serializable
internal data class PhotoPayloadDto(
    val filename: String,
    val clientPhotoUri: String,
    val latitude: Double?,
    val longitude: Double?,
    val description: String?,
)

private fun GeoPoint.toDto() = GeoPointDto(latitude = latitude, longitude = longitude, address = address)

private fun GeoPointDto.toDomain() = GeoPoint(latitude = latitude, longitude = longitude, address = address)

private fun StayPayload.toDto() = StayPayloadDto(latitude = latitude, longitude = longitude, address = address)

private fun StayPayloadDto.toDomain() = StayPayload(latitude = latitude, longitude = longitude, address = address)

private fun MovementPayload.toDto() =
    MovementPayloadDto(
        start = start.toDto(),
        end = end.toDto(),
        distanceMeters = distanceMeters,
        transports = transports.name,
    )

private fun MovementPayloadDto.toDomain() =
    MovementPayload(
        start = start.toDomain(),
        end = end.toDomain(),
        distanceMeters = distanceMeters,
        transports =
            runCatching { MovementPayload.Transport.valueOf(transports) }
                .getOrDefault(MovementPayload.Transport.UNKNOWN),
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
        packageName = packageName,
        title = title,
        text = text,
        collectReason = collectReason.name,
    )

private fun NotificationPayloadDto.toDomain() =
    NotificationPayload(
        appName = appName,
        packageName = packageName,
        title = title,
        text = text,
        collectReason =
            runCatching { NotificationPayload.CollectReason.valueOf(collectReason) }
                .getOrDefault(NotificationPayload.CollectReason.ALL),
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
