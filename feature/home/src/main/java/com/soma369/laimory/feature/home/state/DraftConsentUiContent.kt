package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.StayPayload
import com.soma369.laimory.feature.home.draft.DraftConsentPreparation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 동의 화면 본문의 표시 모델.
 *
 * 생성 시도 스냅샷([DraftConsentPreparation])에서 한 번 파생되며,
 * 요약 건수·유형별 상세가 모두 같은 스냅샷을 근거로 한다.
 */
@Immutable
data class DraftConsentUiContent(
    val attemptId: Long,
    val recordDate: LocalDate,
    val windowText: String,
    val sentTotal: Int,
    val typeSummaries: List<DraftConsentTypeSummary>,
) {
    fun summaryOf(group: DraftConsentTypeGroup): DraftConsentTypeSummary? = typeSummaries.firstOrNull { it.group == group }
}

/** 스냅샷에서 화면 본문을 만든다. 건수는 선택 정책 리포트를 그대로 사용하고 재계산하지 않는다. */
internal fun DraftConsentPreparation.toConsentContent(): DraftConsentUiContent {
    val report = selection.report
    val summaries =
        DraftConsentTypeGroup.entries.map { group ->
            DraftConsentTypeSummary(
                group = group,
                originalCount = group.memberTypes.sumOf { report.originalCounts.getOrDefault(it, 0) },
                sentCount = group.memberTypes.sumOf { report.selectedCounts.getOrDefault(it, 0) },
                sections = buildDetailSections(group, selection.items, zone),
            )
        }
    return DraftConsentUiContent(
        attemptId = attemptId,
        recordDate = recordDate,
        windowText = formatWindow(window.start, window.end, zone),
        sentTotal = report.selectedTotal,
        typeSummaries = summaries,
    )
}

/**
 * 유형별 상세 섹션. Figma 상세 화면의 그룹 구조를 따른다 —
 * 사진·일정은 날짜별, 알림은 앱별, 위치는 체류/이동, 건강은 단일 섹션.
 */
private fun buildDetailSections(
    group: DraftConsentTypeGroup,
    items: List<SourceItem>,
    zone: ZoneId,
): List<DraftConsentDetailSection> =
    when (group) {
        DraftConsentTypeGroup.PHOTO ->
            items
                .filter { it.itemType == ItemType.PHOTO }
                .groupBy { it.startAt.atZone(zone).toLocalDate() }
                .entries
                .sortedByDescending { it.key }
                .map { (date, dateItems) ->
                    DraftConsentDetailSection(
                        title = PHOTO_DATE_FORMAT.format(date),
                        items = dateItems.map { it.toDetailItem(zone) },
                    )
                }

        DraftConsentTypeGroup.CALENDAR ->
            items
                .filter { it.itemType == ItemType.CALENDAR }
                .groupBy { it.startAt.atZone(zone).toLocalDate() }
                .map { (date, dateItems) ->
                    DraftConsentDetailSection(
                        title = DAY_FORMAT.format(date),
                        items = dateItems.map { it.toDetailItem(zone) },
                    )
                }

        DraftConsentTypeGroup.LOCATION ->
            listOf(
                DraftConsentDetailSection(
                    title = "체류한 장소",
                    items = items.filter { it.itemType == ItemType.STAY }.map { it.toDetailItem(zone) },
                ),
                DraftConsentDetailSection(
                    title = "이동 기록",
                    items = items.filter { it.itemType == ItemType.MOVEMENT }.map { it.toDetailItem(zone) },
                ),
            ).filter { it.items.isNotEmpty() }

        DraftConsentTypeGroup.HEALTH ->
            items
                .filter { it.itemType == ItemType.HEALTH }
                .map { it.toDetailItem(zone) }
                .takeIf { it.isNotEmpty() }
                ?.let { listOf(DraftConsentDetailSection(title = null, items = it)) }
                .orEmpty()

        DraftConsentTypeGroup.NOTIFICATION ->
            items
                .filter { it.itemType == ItemType.NOTIFICATION }
                .groupBy { (it.payload as NotificationPayload).appName }
                .map { (appName, appItems) ->
                    DraftConsentDetailSection(
                        title = appName,
                        items = appItems.map { it.toDetailItem(zone) },
                    )
                }
    }

private fun SourceItem.toDetailItem(zone: ZoneId): DraftConsentDetailItem =
    when (val payload = payload) {
        is PhotoPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = payload.fileName,
                description = if (payload.latitude != null && payload.longitude != null) "촬영 위치(EXIF) 포함" else null,
                timeText = DATE_TIME_FORMAT.format(startAt.atZone(zone)),
                imageUri = payload.clientPhotoUri,
            )

        is CalendarPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = payload.title,
                description =
                    listOfNotNull(payload.locationText, payload.description)
                        .filter(String::isNotBlank)
                        .joinToString(" · ")
                        .ifBlank { null },
                timeText = if (payload.allDay) "종일" else formatTimeRange(startAt, endAt, zone),
            )

        is StayPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = payload.address ?: formatCoordinate(payload.latitude, payload.longitude),
                description = if (payload.address != null) formatCoordinate(payload.latitude, payload.longitude) else null,
                timeText = formatDateTimeRange(startAt, endAt, zone),
            )

        is MovementPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = "${payload.start.label()} → ${payload.end.label()}",
                description = "${formatDistance(payload.distanceMeters)} · ${payload.transports.label()}",
                timeText = DATE_TIME_FORMAT.format(startAt.atZone(zone)),
            )

        is HealthPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = payload.metric.label(),
                // 서버 전송 문자열 규칙과 동일: STEPS "8421보" / SLEEP "480분"
                description = "${payload.value.toInt()}${payload.metric.unitLabel()} 전송",
                timeText = formatDateTimeRange(startAt, endAt, zone),
            )

        is NotificationPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = payload.title.orEmpty().ifBlank { "(제목 없음)" },
                description = payload.text?.ifBlank { null },
                timeText = DATE_TIME_FORMAT.format(startAt.atZone(zone)),
            )
    }

private fun GeoPoint.label(): String = address ?: formatCoordinate(latitude, longitude)

private fun MovementPayload.Transport.label(): String =
    when (this) {
        MovementPayload.Transport.WALKING -> "도보"
        MovementPayload.Transport.RUNNING -> "러닝"
        MovementPayload.Transport.ON_BICYCLE -> "자전거"
        MovementPayload.Transport.IN_VEHICLE -> "차량"
        MovementPayload.Transport.UNKNOWN -> "이동"
    }

private fun HealthPayload.Metric.label(): String =
    when (this) {
        HealthPayload.Metric.STEPS -> "걸음 수"
        HealthPayload.Metric.SLEEP -> "수면"
    }

private fun HealthPayload.Metric.unitLabel(): String =
    when (this) {
        HealthPayload.Metric.STEPS -> "보"
        HealthPayload.Metric.SLEEP -> "분"
    }

private fun formatCoordinate(
    latitude: Double,
    longitude: Double,
): String = String.format(Locale.KOREA, "위도 %.5f · 경도 %.5f", latitude, longitude)

private fun formatDistance(distanceMeters: Double): String =
    if (distanceMeters >= 1000) {
        String.format(Locale.KOREA, "%.1fkm", distanceMeters / 1000)
    } else {
        "${distanceMeters.toInt()}m"
    }

private fun formatWindow(
    start: Instant,
    end: Instant,
    zone: ZoneId,
): String = "${DATE_TIME_FORMAT.format(start.atZone(zone))} ~ ${DATE_TIME_FORMAT.format(end.atZone(zone))}"

/** 같은 날짜 그룹 안에서 쓰는 시각 범위: "14:00 ~ 15:00" / 단일 시점은 "14:00". */
private fun formatTimeRange(
    startAt: Instant,
    endAt: Instant?,
    zone: ZoneId,
): String {
    val startText = TIME_FORMAT.format(startAt.atZone(zone))
    val endText = endAt?.let { TIME_FORMAT.format(it.atZone(zone)) } ?: return startText
    return "$startText ~ $endText"
}

/** 날짜가 섞일 수 있는 목록에서 쓰는 범위: "8월 11일 23:50 ~ 00:40" (종료가 다른 날이면 날짜 포함). */
private fun formatDateTimeRange(
    startAt: Instant,
    endAt: Instant?,
    zone: ZoneId,
): String {
    val start = startAt.atZone(zone)
    val startText = DATE_TIME_FORMAT.format(start)
    if (endAt == null) return startText
    val end = endAt.atZone(zone)
    val endText =
        if (start.toLocalDate() == end.toLocalDate()) TIME_FORMAT.format(end) else DATE_TIME_FORMAT.format(end)
    return "$startText ~ $endText"
}

private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREA)
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)
private val PHOTO_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA)
private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREA)
