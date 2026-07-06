package com.soma369.laimory.core.domain.model.collection

/**
 * 카테고리별 수집 원천 데이터.
 *
 * 각 payload 는 서버 업로드 포맷의 `sourceItems[].payload` 후보 필드를 표현한다.
 * 필드 구성은 각 수집기 구현 단계에서 실제 수집 가능 범위에 맞게 조정될 수 있다.
 * 로컬 저장 시에는 JSON 단일 컬럼으로 직렬화되므로, 필드 추가는 스키마 변경 없이 가능하다.
 */
sealed interface SourceItemPayload {
    val itemType: ItemType
}

/** 위경도 좌표 쌍. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

/** 특정 시점 또는 체류 구간의 위치 샘플. */
data class LocationPayload(
    val latitude: Double,
    val longitude: Double,
) : SourceItemPayload {
    override val itemType: ItemType get() = ItemType.LOCATION
}

/**
 * 시작/도착 좌표와 이동 거리, 이동 수단.
 *
 * 원시 수집이 아니라 [LocationPayload] 후처리로 파생되는 데이터이므로,
 * 초기 수집 범위에서는 표현 계약만 정의하고 생성은 확장 단계로 분리한다.
 */
data class MovementPayload(
    val start: GeoPoint,
    val end: GeoPoint,
    val distanceMeters: Double,
    val transport: Transport,
) : SourceItemPayload {
    override val itemType: ItemType get() = ItemType.MOVEMENT

    enum class Transport {
        WALKING,
        IN_VEHICLE,
    }
}

/** 캘린더 일정. */
data class CalendarPayload(
    val title: String,
    val description: String?,
    val locationText: String?,
    val allDay: Boolean,
) : SourceItemPayload {
    override val itemType: ItemType get() = ItemType.CALENDAR
}

/**
 * 걸음수, 수면 등 건강 지표.
 *
 * 계산 가능성을 위해 수치([value])와 단위([unit])를 분리해서 저장하고,
 * "10145보" 같은 표시 문자열은 UI 계층에서 조립한다.
 */
data class HealthPayload(
    val metric: Metric,
    val value: Double,
    val unit: String,
) : SourceItemPayload {
    override val itemType: ItemType get() = ItemType.HEALTH

    enum class Metric {
        STEPS,
        SLEEP,
    }
}

/** 알림 이벤트. 원문 저장 범위는 수집기 구현 단계에서 최소화 정책과 함께 결정한다. */
data class NotificationPayload(
    val appName: String,
    val title: String?,
    val text: String?,
) : SourceItemPayload {
    override val itemType: ItemType get() = ItemType.NOTIFICATION
}

/**
 * 사진 메타데이터.
 *
 * [description] 은 기기 원천 필드가 아니라 후처리/분석 결과이므로
 * 수집 시점에는 null 로 두고 생성 주체를 분리한다.
 */
data class PhotoPayload(
    val fileName: String,
    val clientPhotoUri: String,
    val latitude: Double?,
    val longitude: Double?,
    val description: String?,
) : SourceItemPayload {
    override val itemType: ItemType get() = ItemType.PHOTO
}
