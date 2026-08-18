package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemLimits
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.ui.base.UiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToLong

@Immutable
data class HomeUiState(
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
    val isDraftSheetVisible: Boolean = false,
    val isPhotoSheetVisible: Boolean = false,
    val isDatePickerVisible: Boolean = false,
    val timeSheet: HomeTimeSheetState? = null,
    val draftStatus: DraftCreationStatus = DraftCreationStatus.IDLE,
    val draftRetryMode: DraftRetryMode? = null,
    val draftMessage: String? = null,
    val pastRecords: HomePastRecordsUiState = HomePastRecordsUiState.Loading,
) : UiState {
    fun recordDateWindow(zone: ZoneId): RecordDateWindow? =
        runCatching {
            val endDate = selectedDate.plusDays(endDay.dayOffset.toLong())
            RecordDateWindow(
                start = selectedDate.atTime(startTime).atZone(zone).toInstant(),
                end = endDate.atTime(endTime).atZone(zone).toInstant(),
            )
        }.getOrNull()
}

@Immutable
data class HomePhotoItem(
    val mediaStoreId: Long,
    val uri: String,
    val capturedAt: Instant,
)

@Immutable
data class HomeSourceSummary(
    val photoCount: Int = 0,
    val calendarCount: Int = 0,
    val stepCount: Long = 0,
    val photoPreviewUris: List<String> = emptyList(),
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

internal fun HomeUiState.refreshSourceSummary(
    items: List<SourceItem>,
    photoCandidates: List<PhotoCandidate>,
    zone: ZoneId,
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
    val selectedPhotos = availablePhotos.filter { it.mediaStoreId in selectedIds }
    val steps =
        inWindowNonPhotos
            .mapNotNull { it.payload as? HealthPayload }
            .filter { it.metric == HealthPayload.Metric.STEPS }
            .sumOf { it.value }
            .roundToLong()

    return copy(
        summary =
            HomeSourceSummary(
                photoCount = selectedPhotos.size,
                calendarCount = inWindowNonPhotos.count { it.payload is CalendarPayload },
                stepCount = steps,
                photoPreviewUris = selectedPhotos.take(PHOTO_PREVIEW_LIMIT).map(HomePhotoItem::uri),
                totalItemCount = inWindowNonPhotos.size + selectedPhotos.size,
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

internal fun HomeUiState.nonPhotoSourceItems(
    items: List<SourceItem>,
    zone: ZoneId,
): List<SourceItem> {
    val window = recordDateWindow(zone) ?: return emptyList()
    return items.filter { item -> item.payload !is PhotoPayload && window.contains(item) }
}

internal const val MAX_PHOTO_SELECTION = DraftSourceItemLimits.DEFAULT_PHOTO
private const val PHOTO_PREVIEW_LIMIT = 3
