package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
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
    val selectedPhotoIds: Set<String> = emptySet(),
    val pendingPhotoIds: Set<String> = emptySet(),
    val hasCustomizedPhotoSelection: Boolean = false,
    val isDraftSheetVisible: Boolean = false,
    val isPhotoSheetVisible: Boolean = false,
    val isDatePickerVisible: Boolean = false,
    val editingTimeField: HomeTimeField? = null,
    val draftStatus: DraftCreationStatus = DraftCreationStatus.IDLE,
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
    val rawId: String,
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

enum class DraftEndDay(
    val dayOffset: Int,
) {
    SAME_DAY(0),
    NEXT_DAY(1),
}

enum class HomeTimeField {
    START,
    END,
}

enum class DraftCreationStatus {
    IDLE,
    SUBMITTING,
    SUBMITTED,
    FAILED,
}

internal fun HomeUiState.refreshSourceSummary(
    items: List<SourceItem>,
    zone: ZoneId,
    resetPhotoSelection: Boolean = false,
): HomeUiState {
    val window =
        recordDateWindow(zone)
            ?: return copy(
                summary = HomeSourceSummary(),
                availablePhotos = emptyList(),
                selectedPhotoIds = emptySet(),
                pendingPhotoIds = emptySet(),
                hasCustomizedPhotoSelection = false,
            )
    val inWindow = items.filter(window::contains)
    val photoItems =
        inWindow
            .filter { it.payload is PhotoPayload }
            .sortedByDescending(SourceItem::startAt)
    val availablePhotos =
        photoItems.map { item ->
            HomePhotoItem(
                rawId = item.rawId,
                uri = (item.payload as PhotoPayload).clientPhotoUri,
                capturedAt = item.startAt,
            )
        }
    val availableIds = availablePhotos.mapTo(linkedSetOf(), HomePhotoItem::rawId)
    val resetSelection = resetPhotoSelection || !hasCustomizedPhotoSelection
    val selectedIds =
        if (resetSelection) {
            availableIds
        } else {
            selectedPhotoIds.intersect(availableIds)
        }
    val selectedPhotos = availablePhotos.filter { it.rawId in selectedIds }
    val steps =
        inWindow
            .mapNotNull { it.payload as? HealthPayload }
            .filter { it.metric == HealthPayload.Metric.STEPS }
            .sumOf { it.value }
            .roundToLong()
    val nonPhotoCount = inWindow.count { it.payload !is PhotoPayload }

    return copy(
        summary =
            HomeSourceSummary(
                photoCount = selectedPhotos.size,
                calendarCount = inWindow.count { it.payload is CalendarPayload },
                stepCount = steps,
                photoPreviewUris = selectedPhotos.take(PHOTO_PREVIEW_LIMIT).map(HomePhotoItem::uri),
                totalItemCount = nonPhotoCount + selectedPhotos.size,
            ),
        availablePhotos = availablePhotos,
        selectedPhotoIds = selectedIds,
        pendingPhotoIds =
            if (isPhotoSheetVisible) {
                pendingPhotoIds.intersect(availableIds)
            } else {
                emptySet()
            },
        hasCustomizedPhotoSelection = if (resetSelection) false else hasCustomizedPhotoSelection,
    )
}

internal fun HomeUiState.selectedSourceItems(
    items: List<SourceItem>,
    zone: ZoneId,
): List<SourceItem> {
    val window = recordDateWindow(zone) ?: return emptyList()
    return items.filter { item ->
        window.contains(item) && (item.payload !is PhotoPayload || item.rawId in selectedPhotoIds)
    }
}

private const val PHOTO_PREVIEW_LIMIT = 3
