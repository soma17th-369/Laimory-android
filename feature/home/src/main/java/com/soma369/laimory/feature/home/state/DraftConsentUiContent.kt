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
    /** 위치 상세 지도에 찍을 마커. 현재 생성 시도 스냅샷에서만 나오며 다른 날짜를 조회하지 않는다. */
    val locationMarkers: List<ConsentLocationMarker>,
) {
    fun summaryOf(group: DraftConsentTypeGroup): DraftConsentTypeSummary? = typeSummaries.firstOrNull { it.group == group }

    /** 현재 생성 시도의 위치 항목 rawId. 위치정보 전송 Switch 가 한 번에 켜고 끄는 대상이다. */
    val locationRawIds: Set<String> get() = locationMarkers.mapTo(linkedSetOf()) { it.sourceRawId }
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
        locationMarkers = selection.items.toLocationMarkers(zone),
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
                .groupBy { (it.payload as NotificationPayload).packageName }
                .entries
                // 표시명이 같아도 패키지가 다르면 다른 앱이므로 섹션을 나눈다.
                // 순서는 각 앱의 가장 최근 알림 기준 내림차순 — 같으면 패키지명으로 고정한다.
                .sortedWith(
                    compareByDescending<Map.Entry<String, List<SourceItem>>> { (_, appItems) ->
                        appItems.maxOf(SourceItem::startAt)
                    }.thenBy { (packageName, _) -> packageName },
                )
                .map { (packageName, appItems) ->
                    DraftConsentDetailSection(
                        // 앱 이름이 바뀌었을 수 있어 가장 최근 알림의 수집 당시 표시명을 쓴다.
                        title = (appItems.maxBy(SourceItem::startAt).payload as NotificationPayload).appName,
                        items = appItems.map { it.toDetailItem(zone) },
                        iconPackageName = packageName,
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
                title = payload.address ?: UNRESOLVED_PLACE_LABEL,
                description = null,
                timeText = formatDateTimeRange(startAt, endAt, zone),
            )

        /*
         * 체류와 같은 형태로 맞춘다 — 시각은 시작~종료 범위로 제목 아래에 둔다.
         *
         * 출발지와 도착지는 줄을 나눈다. 주소가 둘 다 길어 한 줄에 붙이면 어디서 끊겼는지
         * 읽기 어렵다. 화살표는 첫 줄 끝에 남겨 다음 줄이 도착지임을 알린다.
         *
         * 거리와 이동수단은 시각 줄 오른쪽 끝에 함께 붙인다. 둘 다 짧아 한 덩어리로 읽히고,
         * 설명 줄을 따로 두지 않아 카드가 한 줄 짧아진다.
         */
        is MovementPayload ->
            DraftConsentDetailItem(
                key = rawId,
                title = "${payload.start.label()} →\n${payload.end.label()}",
                description = null,
                timeText = formatDateTimeRange(startAt, endAt, zone),
                trailingText = "${formatDistance(payload.distanceMeters)} · ${payload.transports.label()}",
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

/** 위경도 좌표는 전송 항목이지만 화면에는 노출하지 않고 주소 요약만 표시한다. */
private fun GeoPoint.label(): String = address ?: UNRESOLVED_PLACE_LABEL

private const val UNRESOLVED_PLACE_LABEL = "주소 미확인"

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

/**
 * 날짜가 섞일 수 있는 목록에서 쓰는 범위: "8월 11일 23:50 ~ 00:40" (종료가 다른 날이면 날짜 포함).
 *
 * 위치 마커 설명도 목록과 같은 시각 문구를 쓰도록 모듈 안에서 공유한다.
 */
internal fun formatDateTimeRange(
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
