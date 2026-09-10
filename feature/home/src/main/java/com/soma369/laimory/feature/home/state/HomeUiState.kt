package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemLimits
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.ui.base.UiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Immutable
data class HomeUiState(
    /** 수집 실험실 진입 가능 여부. 개발 도구라 debug 빌드에서만 참이다. */
    val isCollectionLabAccessible: Boolean = false,
    /** 인사말에 쓸 닉네임. 조회 전·없음·실패를 구분하지 않는다 — 어느 쪽이든 문구가 같다. */
    val nickname: String? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.MIDNIGHT,
    val endDay: DraftEndDay = DraftEndDay.NEXT_DAY,
    val endTime: LocalTime = LocalTime.MIDNIGHT,
    val summary: HomeSourceSummary = HomeSourceSummary(),
    val availablePhotos: List<HomePhotoItem> = emptyList(),
    val selectedPhotoIds: Set<Long> = emptySet(),
    val pendingPhotoIds: Set<Long> = emptySet(),
    val isPhotoLoading: Boolean = false,
    val isPhotoAccessLimited: Boolean = false,
    /**
     * 사진 접근을 거부당한 채로 시트를 연 상태.
     *
     * 거부됐다고 시트를 안 열면 초안 만들기를 눌렀는데 아무 일도 일어나지 않는다. 열어서
     * 왜 비었는지 알리고, 설정으로 나가거나 사진 없이 이어 가도록 둔다.
     */
    val isPhotoAccessDenied: Boolean = false,
    val isPhotoSheetVisible: Boolean = false,
    val isDatePickerVisible: Boolean = false,
    val timeSheet: HomeTimeSheetState? = null,
    val draftStatus: DraftCreationStatus = DraftCreationStatus.IDLE,
    val draftRetryMode: DraftRetryMode? = null,
    val draftMessage: String? = null,
    val pastRecords: HomePastRecordsUiState = HomePastRecordsUiState.Loading,
    /**
     * 이미 저장이 끝난 기록의 날짜. 날짜 피커에서 고를 수 없게 하는 데 쓴다.
     *
     * 서버가 그 날짜의 초안 생성을 409 `-1003` 으로 거절하므로, 고르게 두면 사진까지 다 고른 뒤
     * 마지막에야 막힌다. **초안(DRAFT) 날짜는 여기 없다** — 서버가 이어 붙이기로 받아 주므로
     * 다시 만들 수 있는 날이다.
     *
     * 피커가 보여 주는 달만 담긴다. 아직 받지 못한 달은 비어 있고, 그때는 서버 거절이 최후
     * 방어선으로 남는다.
     */
    val savedRecordDates: Set<LocalDate> = emptySet(),
) : UiState {
    /**
     * 지금 설정으로 만들어지는 기록 창. 정책을 벗어나면 null 이라 초안 생성이 막힌다.
     *
     * 최소 길이·종료 허용 범위는 [DraftWindowPolicy]가 갖는다 — 공용 [RecordDateWindow] 에 넣으면
     * 사진 조회·수집 선별 등 같은 모델을 쓰는 다른 경로까지 함께 좁아진다.
     */
    fun recordDateWindow(zone: ZoneId): RecordDateWindow? {
        val endDateTime = selectedDate.plusDays(endDay.dayOffset.toLong()).atTime(endTime)
        if (!DraftWindowPolicy.isValid(selectedDate, startTime, endDateTime)) return null
        return runCatching {
            RecordDateWindow(
                start = selectedDate.atTime(startTime).atZone(zone).toInstant(),
                end = endDateTime.atZone(zone).toInstant(),
            )
        }.getOrNull()
    }
}

@Immutable
data class HomePhotoItem(
    val mediaStoreId: Long,
    val uri: String,
    val capturedAt: Instant,
)

/**
 * 홈 원천 카드가 그리는 값.
 *
 * 카드 본문은 유형마다 `M개 중 N개` 이고, 그 위 내용 슬롯은 유형마다 다르다 — 사진은 격자,
 * 일정·알림은 3초마다 넘기는 목록이다. 걸음(건강)은 카드에 없어 여기서도 빠진다. 전송은 그대로다.
 */
@Immutable
data class HomeSourceSummary(
    val photo: HomeSourceCount = HomeSourceCount(),
    val calendar: HomeSourceCount = HomeSourceCount(),
    val location: HomeSourceCount = HomeSourceCount(),
    val notification: HomeSourceCount = HomeSourceCount(),
    /** 사진 격자에 그릴 후보. **선택분이 아니라 후보 기준**이다 — 카드는 무엇이 모였는지를 보여 준다. */
    val photoPreviewUris: List<String> = emptyList(),
    val calendarItems: List<HomeCalendarItem> = emptyList(),
    val notificationApps: List<HomeNotificationApp> = emptyList(),
    val totalItemCount: Int = 0,
)

/** 기록 창의 종료일. [label]은 기록 날짜를 기준으로 한 상대 표현이라 화면·피커가 함께 쓴다. */
enum class DraftEndDay(
    val dayOffset: Int,
    val label: String,
) {
    SAME_DAY(0, "당일"),
    NEXT_DAY(1, "익일"),
}

enum class HomeTimeField {
    START,
    END,
}

/** 제출 중 상태는 동의 화면이 소유한다 — 홈은 코디네이터 추적 상태만 반영한다. */
enum class DraftCreationStatus {
    IDLE,
    PROCESSING,
    LONG_RUNNING,
    SUCCESS,
    FAILED,
}

enum class DraftRetryMode {
    POLLING,
    NEW_DRAFT,
}

internal val DraftCreationStatus.isInProgress: Boolean
    get() = this == DraftCreationStatus.PROCESSING

internal val DraftCreationStatus.isDateLocked: Boolean
    get() = isInProgress || this == DraftCreationStatus.LONG_RUNNING

internal val DraftCreationStatus.isInputLocked: Boolean
    get() = isDateLocked || this == DraftCreationStatus.SUCCESS

/**
 * 기록 창 안에 모인 것을 홈 카드가 그릴 값으로 옮긴다.
 *
 * [selection] 은 홈이 상시로 돌린 선택 정책 결과다. 전송 예정 수(N)는 타입별 상한과 사용자 제외를
 * 반영한 값이라 후보 수(M)만으로는 알 수 없다. 아직 없으면 N 을 0 으로 둔다 — 없는 값을 후보 수로
 * 대신 채우면 카드가 실제보다 많이 보낸다고 말하게 된다.
 *
 * [excludedRawIds] 는 사용자가 상세에서 뺀 항목이다. 소유는 세션 스토어로 옮겨가며(후속 이슈),
 * 여기서는 입력으로 받아 N 에서 덜어 낸다.
 */
internal fun HomeUiState.refreshSourceSummary(
    items: List<SourceItem>,
    photoCandidates: List<PhotoCandidate>,
    zone: ZoneId,
    selection: DraftSourceItemSelection? = null,
    excludedRawIds: Set<String> = emptySet(),
): HomeUiState {
    val window =
        recordDateWindow(zone)
            ?: return copy(
                summary = HomeSourceSummary(),
                availablePhotos = emptyList(),
                selectedPhotoIds = emptySet(),
                pendingPhotoIds = emptySet(),
            )
    val inWindowNonPhotos =
        items.filter { item ->
            item.payload !is PhotoPayload && window.contains(item)
        }
    val availablePhotos =
        photoCandidates
            .asSequence()
            .filter { candidate -> candidate.takenAt >= window.start && candidate.takenAt < window.end }
            .sortedByDescending(PhotoCandidate::takenAt)
            .map { candidate ->
                HomePhotoItem(
                    mediaStoreId = candidate.id,
                    uri = candidate.contentUri,
                    capturedAt = candidate.takenAt,
                )
            }.toList()
    val availableIds = availablePhotos.mapTo(linkedSetOf(), HomePhotoItem::mediaStoreId)
    val selectedIds = selectedPhotoIds.intersect(availableIds)

    return copy(
        summary =
            HomeSourceSummary(
                // 사진은 자동 절삭이 없어 전송 예정 수가 곧 고른 수다.
                photo = HomeSourceCount(candidate = availablePhotos.size, sending = selectedIds.size),
                calendar = countOf(DraftConsentTypeGroup.CALENDAR, inWindowNonPhotos, selection, excludedRawIds),
                location = countOf(DraftConsentTypeGroup.LOCATION, inWindowNonPhotos, selection, excludedRawIds),
                notification = countOf(DraftConsentTypeGroup.NOTIFICATION, inWindowNonPhotos, selection, excludedRawIds),
                photoPreviewUris = availablePhotos.take(PHOTO_PREVIEW_LIMIT).map(HomePhotoItem::uri),
                calendarItems = inWindowNonPhotos.toCalendarItems(),
                notificationApps = inWindowNonPhotos.toNotificationApps(),
                totalItemCount = inWindowNonPhotos.size + selectedIds.size,
            ),
        availablePhotos = availablePhotos,
        selectedPhotoIds = selectedIds,
        pendingPhotoIds =
            if (isPhotoSheetVisible) {
                pendingPhotoIds.intersect(availableIds)
            } else {
                emptySet()
            },
    )
}

/** 한 유형의 `M개 중 N개`. 위치는 STAY·MOVEMENT 를 합친 한 건수다. */
private fun countOf(
    group: DraftConsentTypeGroup,
    inWindowNonPhotos: List<SourceItem>,
    selection: DraftSourceItemSelection?,
    excludedRawIds: Set<String>,
): HomeSourceCount {
    val types = group.memberTypes.toSet()
    val sending =
        selection
            ?.items
            .orEmpty()
            .count { it.itemType in types && it.rawId !in excludedRawIds }
    return HomeSourceCount(
        candidate = inWindowNonPhotos.count { it.itemType in types },
        sending = sending,
    )
}

/** 시작 시각 오름차순. 같은 시각이면 `rawId` 로 고정해 회전 순서가 흔들리지 않게 한다. */
private fun List<SourceItem>.toCalendarItems(): List<HomeCalendarItem> =
    asSequence()
        .mapNotNull { item ->
            val payload = item.payload as? CalendarPayload ?: return@mapNotNull null
            HomeCalendarItem(
                rawId = item.rawId,
                title = payload.title,
                startAt = item.startAt,
                endAt = item.endAt,
                allDay = payload.allDay,
            )
        }.sortedWith(compareBy({ it.startAt }, { it.rawId }))
        .toList()

/**
 * 앱별 알림 건수. 건수 내림차순, 같으면 패키지명으로 고정한다.
 *
 * 표시명은 **가장 최근 알림의 수집 당시 이름**이다 — 앱 이름이 바뀌었을 수 있어 지금 이름을 다시
 * 읽지 않는다. 표시명이 같아도 패키지가 다르면 다른 앱이다.
 */
private fun List<SourceItem>.toNotificationApps(): List<HomeNotificationApp> =
    asSequence()
        .filter { it.payload is NotificationPayload }
        .groupBy { (it.payload as NotificationPayload).packageName }
        .map { (packageName, appItems) ->
            HomeNotificationApp(
                packageName = packageName,
                appName = (appItems.maxBy(SourceItem::startAt).payload as NotificationPayload).appName,
                count = appItems.size,
            )
        }.sortedWith(compareByDescending<HomeNotificationApp> { it.count }.thenBy { it.packageName })

internal fun HomeUiState.nonPhotoSourceItems(
    items: List<SourceItem>,
    zone: ZoneId,
): List<SourceItem> {
    val window = recordDateWindow(zone) ?: return emptyList()
    return items.filter { item -> item.payload !is PhotoPayload && window.contains(item) }
}

internal const val MAX_PHOTO_SELECTION = DraftSourceItemLimits.DEFAULT_PHOTO
private const val PHOTO_PREVIEW_LIMIT = 3
