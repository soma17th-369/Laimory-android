package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.timeline.model.TimelineEventUiModel
import com.soma369.laimory.feature.timeline.model.TimelineItemCountUiModel
import com.soma369.laimory.feature.timeline.state.TimelineMemoEditorState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.soma369.laimory.core.ui.R as UiR

@Composable
internal fun TimelineEventCard(
    event: TimelineEventUiModel,
    onEditClick: () -> Unit,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
    isEditable: Boolean = true,
    memoEditor: TimelineMemoEditorState? = null,
    onMemoClick: () -> Unit = {},
    onMemoChange: (String) -> Unit = {},
    onMemoCancel: () -> Unit = {},
    onMemoConfirm: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val photoSlots = event.photoSlots()
    val indicatorColor = MaterialTheme.colorScheme.primary
    val indicatorLineColor = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = event.timeLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.small))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .drawWithCache {
                        val indicatorRadius = 5.dp.toPx()
                        val indicatorCenter = Offset(indicatorRadius, indicatorRadius)
                        val lineStartY = 12.dp.toPx()
                        val lineWidth = 1.dp.toPx()
                        onDrawBehind {
                            drawCircle(
                                color = indicatorColor,
                                radius = indicatorRadius,
                                center = indicatorCenter,
                            )
                            if (size.height > lineStartY) {
                                drawLine(
                                    color = indicatorLineColor,
                                    start = Offset(indicatorCenter.x, lineStartY),
                                    end = Offset(indicatorCenter.x, size.height),
                                    strokeWidth = lineWidth,
                                )
                            }
                        }
                    }.padding(start = 18.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                if (event.eventType == TimelineEventType.PHOTO_MOMENT && photoSlots.isNotEmpty()) {
                    PhotoMainEvent(
                        event = event,
                        photoSlots = photoSlots,
                        onEditClick = onEditClick,
                        onPhotoClick = onPhotoClick,
                        isEditable = isEditable,
                        memoEditor = memoEditor,
                        onMemoClick = onMemoClick,
                        onMemoChange = onMemoChange,
                        onMemoCancel = onMemoCancel,
                        onMemoConfirm = onMemoConfirm,
                    )
                } else {
                    EventMainCard(event = event, onEditClick = onEditClick, isEditable = isEditable)
                    if (photoSlots.isNotEmpty()) {
                        PhotoThumbnailRow(
                            photoSlots = photoSlots,
                            onPhotoClick = onPhotoClick,
                        )
                    }
                    event.nonPhotoItemCountLabel()?.let { label ->
                        Text(
                            text = label,
                            modifier =
                                Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape,
                                    ).padding(horizontal = 10.dp, vertical = Spacing.extraSmall),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TimelineMemo(
                        memo = event.memo,
                        editor = memoEditor,
                        isEditable = isEditable,
                        onClick = onMemoClick,
                        onValueChange = onMemoChange,
                        onCancel = onMemoCancel,
                        onConfirm = onMemoConfirm,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventMainCard(
    event: TimelineEventUiModel,
    onEditClick: () -> Unit,
    isEditable: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(event.eventType.iconResource()),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = event.title.ifBlank { event.eventType.label() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                event.subtitle?.takeIf(String::isNotBlank)?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isEditable) TimelineEditButton(onClick = onEditClick)
        }
    }
}

@Composable
private fun PhotoMainEvent(
    event: TimelineEventUiModel,
    photoSlots: List<String?>,
    onEditClick: () -> Unit,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
    isEditable: Boolean,
    memoEditor: TimelineMemoEditorState?,
    onMemoClick: () -> Unit,
    onMemoChange: (String) -> Unit,
    onMemoCancel: () -> Unit,
    onMemoConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val photoWidth = (maxWidth - Spacing.medium) / 2
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                itemsIndexed(photoSlots) { index, photoUrl ->
                    TimelinePhoto(
                        photoUrl = photoUrl,
                        onClick = { onPhotoClick(photoSlots, index) },
                        modifier = Modifier.size(photoWidth),
                        cornerRadius = 12.dp,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoLocationChip(
                label = event.photoLocationLabel(),
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isEditable) TimelineEditButton(onClick = onEditClick)
        }
        TimelineMemo(
            memo = event.memo,
            editor = memoEditor,
            isEditable = isEditable,
            onClick = onMemoClick,
            onValueChange = onMemoChange,
            onCancel = onMemoCancel,
            onConfirm = onMemoConfirm,
        )
    }
}

@Composable
private fun PhotoThumbnailRow(
    photoSlots: List<String?>,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(photoSlots) { index, photoUrl ->
            TimelinePhoto(
                photoUrl = photoUrl,
                onClick = { onPhotoClick(photoSlots, index) },
                modifier = Modifier.size(64.dp),
                cornerRadius = 4.dp,
            )
        }
    }
}

@Composable
private fun TimelinePhoto(
    photoUrl: String?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier,
    cornerRadius: Dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isInPreview = LocalInspectionMode.current
    Box(
        modifier =
            modifier
                .clip(shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (!isInPreview && photoUrl != null) {
            TimelineAsyncImage(photoUrl = photoUrl)
        }
    }
}

@Composable
private fun TimelineAsyncImage(photoUrl: String) {
    AsyncImage(
        model = photoUrl,
        contentDescription = "타임라인 사진",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun PhotoLocationChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(5.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TimelineEditButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(28.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(UiR.drawable.ico_timeline_tool_edit),
                contentDescription = "이벤트 편집",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun TimelineEventUiModel.timeLabel(): String {
    val start = startAt.format(TimeFormatter)
    val end = endAt?.format(TimeFormatter)
    return if (end == null) start else "$start ~ $end"
}

private fun TimelineEventUiModel.photoSlots(): List<String?> {
    val photoCount = itemCounts.firstOrNull { it.itemType == TimelineItemType.PHOTO }?.count ?: 0
    return List(photoCount) { index -> photoUrls.getOrNull(index) }
}

private fun TimelineEventUiModel.nonPhotoItemCountLabel(): String? =
    itemCounts
        .filterNot { it.itemType == TimelineItemType.PHOTO }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(separator = " · ") { item ->
            "${item.itemType.label()} ${item.count}개"
        }

private fun TimelineEventUiModel.photoLocationLabel(): String =
    subtitle?.takeIf(String::isNotBlank)
        ?: title.takeIf(String::isNotBlank)
        ?: eventType.label()

private fun TimelineItemType.label(): String =
    when (this) {
        TimelineItemType.PHOTO -> "사진"
        TimelineItemType.CALENDAR -> "일정"
        TimelineItemType.STAY -> "체류"
        TimelineItemType.MOVEMENT -> "이동"
        TimelineItemType.HEALTH -> "건강"
        TimelineItemType.NOTIFICATION -> "알림"
        TimelineItemType.UNKNOWN -> "기타"
    }

internal fun TimelineEventType.label(): String = displayLabel()

@Preview(name = "이벤트 타입별", showBackground = true, widthDp = 360)
@Composable
private fun TimelineEventTypeCardPreview(
    @PreviewParameter(TimelineEventPreviewParameterProvider::class)
    event: TimelineEventUiModel,
) {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = event,
                onEditClick = {},
                onPhotoClick = { _, _ -> },
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

@Preview(name = "일반 이벤트 사진 개수별", showBackground = true, widthDp = 360)
@Composable
private fun PhotoThumbnailEventCardPreview(
    @PreviewParameter(PhotoThumbnailCountPreviewParameterProvider::class)
    photoCount: Int,
) {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = photoThumbnailPreviewEvent(photoCount),
                onEditClick = {},
                onPhotoClick = { _, _ -> },
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

@Preview(name = "사진 메인 개수별", showBackground = true, widthDp = 360)
@Composable
private fun PhotoMainEventCardPreview(
    @PreviewParameter(PhotoMainCountPreviewParameterProvider::class)
    photoCount: Int,
) {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = photoMainPreviewEvent(photoCount),
                onEditClick = {},
                onPhotoClick = { _, _ -> },
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

@Preview(name = "메모 편집 상태", showBackground = true, widthDp = 360)
@Composable
private fun TimelineMemoEditorPreview(
    @PreviewParameter(TimelineMemoEditorPreviewParameterProvider::class)
    editor: TimelineMemoEditorState,
) {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = photoThumbnailPreviewEvent(photoCount = 0),
                onEditClick = {},
                onPhotoClick = { _, _ -> },
                memoEditor = editor,
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

internal class TimelineEventPreviewParameterProvider : PreviewParameterProvider<TimelineEventUiModel> {
    override val values: Sequence<TimelineEventUiModel> =
        TimelineEventType.entries.asSequence().map { eventType ->
            if (eventType == TimelineEventType.PHOTO_MOMENT) {
                photoMainPreviewEvent()
            } else {
                defaultPreviewEvent(eventType)
            }
        }
}

internal class PhotoThumbnailCountPreviewParameterProvider : PreviewParameterProvider<Int> {
    override val values: Sequence<Int> = sequenceOf(0, 1, 2, 3, 5)
}

internal class PhotoMainCountPreviewParameterProvider : PreviewParameterProvider<Int> {
    override val values: Sequence<Int> = sequenceOf(1, 2, 5)
}

internal class TimelineMemoEditorPreviewParameterProvider : PreviewParameterProvider<TimelineMemoEditorState> {
    override val values: Sequence<TimelineMemoEditorState> =
        sequenceOf(
            TimelineMemoEditorState(
                timelineEventId = 2L,
                originalMemo = "7호선이 평소보다 많이 붐볐다.",
                draftMemo = "오늘은 비가 와서 조금 우울했지만, 카페에서 따뜻한 라떼를 마시니 기분이 좋아졌다.",
            ),
            TimelineMemoEditorState(
                timelineEventId = 2L,
                originalMemo = "7호선이 평소보다 많이 붐볐다.",
                draftMemo = "저장 중인 메모",
                isSaving = true,
            ),
            TimelineMemoEditorState(
                timelineEventId = 2L,
                originalMemo = "",
                draftMemo = "가".repeat(10_001),
            ),
        )
}

private fun defaultPreviewEvent(eventType: TimelineEventType) =
    TimelineEventUiModel(
        timelineEventId = eventType.ordinal.toLong(),
        eventType = eventType,
        startAt = LocalDateTime.of(2026, 5, 8, 9, 0).plusHours(eventType.ordinal.toLong()),
        endAt = null,
        title = eventType.label(),
        subtitle = "타입별 타임라인 이벤트",
        memo = "이벤트 타입에 맞는 아이콘과 텍스트 스타일을 확인해요.",
        itemCounts = emptyList(),
    )

private fun photoMainPreviewEvent(photoCount: Int = 5) =
    TimelineEventUiModel(
        timelineEventId = 1L,
        eventType = TimelineEventType.PHOTO_MOMENT,
        startAt = LocalDateTime.of(2026, 5, 8, 18, 20),
        endAt = null,
        title = "친구와 카페",
        subtitle = "성수동 · 작은 카페",
        memo = "오랜만에 만난 고등학교 친구와 만나서 딸기라떼 먹었다. 양이 너무 적었지만 맛있었다.",
        itemCounts = listOf(TimelineItemCountUiModel(TimelineItemType.PHOTO, photoCount)),
        photoUrls = List(photoCount) { null },
    )

private fun photoThumbnailPreviewEvent(photoCount: Int = 5) =
    TimelineEventUiModel(
        timelineEventId = 2L,
        eventType = TimelineEventType.MOVEMENT,
        startAt = LocalDateTime.of(2026, 5, 8, 8, 10),
        endAt = LocalDateTime.of(2026, 5, 8, 8, 45),
        title = "출근길",
        subtitle = "강남역 → 성수역 · 7호선",
        memo = "7호선이 평소보다 많이 붐볐다.",
        itemCounts = listOf(TimelineItemCountUiModel(TimelineItemType.PHOTO, photoCount)),
        photoUrls = List(photoCount) { null },
    )
