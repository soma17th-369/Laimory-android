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
    val photoPreviewUris: List<String>,
)

/** 스냅샷에서 화면 본문을 만든다. 건수는 선택 정책 리포트를 그대로 사용하고 재계산하지 않는다. */
internal fun DraftConsentPreparation.toConsentContent(): DraftConsentUiContent {
    val report = selection.report
    val itemsByType = selection.items.groupBy(SourceItem::itemType)
    val summaries =
        DraftConsentTypeGroup.entries.map { group ->
            DraftConsentTypeSummary(
                group = group,
                originalCount = group.memberTypes.sumOf { report.originalCounts.getOrDefault(it, 0) },
                sentCount = group.memberTypes.sumOf { report.selectedCounts.getOrDefault(it, 0) },
                sections = buildDetailSections(group, itemsByType, zone),
            )
        }
    return DraftConsentUiContent(
        attemptId = attemptId,
        recordDate = recordDate,
        windowText = formatWindow(window.start, window.end, zone),
        sentTotal = report.selectedTotal,
        typeSummaries = summaries,
        photoPreviewUris =
            selection.items
                .mapNotNull { (it.payload as? PhotoPayload)?.clientPhotoUri }
                .take(CONSENT_PHOTO_PREVIEW_LIMIT),
    )
}

private fun buildDetailSections(
    group: DraftConsentTypeGroup,
    itemsByType: Map<ItemType, List<SourceItem>>,
    zone: ZoneId,
): List<DraftConsentDetailSection> {
    if (group == DraftConsentTypeGroup.LOCATION) {
        return listOf(
            DraftConsentDetailSection(
                title = "체류",
                items = itemsByType.detailItems(ItemType.STAY, zone),
            ),
            DraftConsentDetailSection(
                title = "이동",
                items = itemsByType.detailItems(ItemType.MOVEMENT, zone),
            ),
        ).filter { it.items.isNotEmpty() }
    }
    val itemType = group.memberTypes.single()
    val items = itemsByType.detailItems(itemType, zone)
    return if (items.isEmpty()) emptyList() else listOf(DraftConsentDetailSection(title = null, items = items))
}

private fun Map<ItemType, List<SourceItem>>.detailItems(
    itemType: ItemType,
    zone: ZoneId,
): List<DraftConsentDetailItem> = getOrDefault(itemType, emptyList()).map { it.toDetailItem(zone) }

private fun SourceItem.toDetailItem(zone: ZoneId): DraftConsentDetailItem {
    val timeText = formatItemTime(startAt, endAt, zone)
    return when (val payload = payload) {
        is PhotoPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = payload.fileName,
                description = if (payload.latitude != null && payload.longitude != null) "촬영 위치(EXIF) 포함" else null,
                timeText = timeText,
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
                timeText = timeText,
            )

        is StayPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = payload.address ?: formatCoordinate(payload.latitude, payload.longitude),
                description = if (payload.address != null) formatCoordinate(payload.latitude, payload.longitude) else null,
                timeText = timeText,
            )

        is MovementPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = "${payload.start.label()} → ${payload.end.label()}",
                description = "${payload.transports.label()} · ${formatDistance(payload.distanceMeters)}",
                timeText = timeText,
            )

        is HealthPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = payload.metric.label(),
                // 서버 전송 문자열 규칙과 동일: STEPS "8421보" / SLEEP "480분"
                description = "${payload.value.toInt()}${payload.metric.unitLabel()} 전송",
                timeText = timeText,
            )

        is NotificationPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = "[${payload.appName}] ${payload.title.orEmpty().ifBlank { "(제목 없음)" }}",
                description = payload.text?.ifBlank { null },
                timeText = timeText,
            )
    }
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
): String = "${CONSENT_DATE_TIME_FORMAT.format(start.atZone(zone))} ~ ${CONSENT_DATE_TIME_FORMAT.format(end.atZone(zone))}"

private fun formatItemTime(
    startAt: Instant,
    endAt: Instant?,
    zone: ZoneId,
): String {
    val start = startAt.atZone(zone)
    val startText = CONSENT_DATE_TIME_FORMAT.format(start)
    if (endAt == null) return startText
    val end = endAt.atZone(zone)
    val endText =
        if (start.toLocalDate() == end.toLocalDate()) {
            CONSENT_TIME_FORMAT.format(end)
        } else {
            CONSENT_DATE_TIME_FORMAT.format(end)
        }
    return "$startText ~ $endText"
}

private val CONSENT_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREA)
private val CONSENT_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)
private const val CONSENT_PHOTO_PREVIEW_LIMIT = 3
