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

/**
 * 특정 위치에 일정 시간 머문 위치/체류 이벤트.
 *
 * 체류 시간은 payload 에 중복 저장하지 않고 [SourceItem.startAt]/[SourceItem.endAt] 에서
 * 파생한다. 업로드 payload 에 체류 시간 필드가 필요하면 projection 시 계산해서 채운다.
 */
data class LocationPayload(
    val latitude: Double,
    val longitude: Double,
) : SourceItemPayload {
    override val itemType: ItemType get() = ItemType.LOCATION
}

/**
 * 시작 위치와 도착 위치를 가지는 이동 이벤트.
 *
 * 원시 수집이 아니라 [LocationPayload] 후처리로 파생되는 데이터이므로,
 * 초기 수집 범위에서는 표현 계약만 정의하고 생성은 확장 단계로 분리한다.
 * 이동 거리와 이동 수단 추론은 후속 확장이며 이 계약에 포함하지 않는다.
 */
data class MovementPayload(
    val start: GeoPoint,
    val end: GeoPoint,
) : SourceItemPayload {
    override val itemType: ItemType get() = ItemType.MOVEMENT
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
 * 계산 가능성을 위해 수치([value])와 단위([unit])를 분리해서 저장한다.
 * 업로드 payload 의 "xx보" 같은 표시 문자열은 projection 시 value+unit 으로 렌더링한다.
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

/**
 * 알림 이벤트.
 *
 * allowlist 와 sourceKey 구성의 기준은 [packageName] 이다 — [appName] 은 표시명이라
 * 중복되거나 바뀔 수 있다. 서버 업로드 시 일부 필드는 projection 에서 제외될 수 있다.
 * 원문 저장 범위는 수집기 구현 단계에서 최소화 정책과 함께 결정한다.
 */
data class NotificationPayload(
    val appName: String,
    val packageName: String,
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
