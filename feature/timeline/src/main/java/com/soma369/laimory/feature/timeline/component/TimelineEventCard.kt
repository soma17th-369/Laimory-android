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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
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
import com.soma369.laimory.core.ui.theme.laimorySignature
import com.soma369.laimory.feature.timeline.model.TimelineEventUiModel
import com.soma369.laimory.feature.timeline.model.TimelineItemCountUiModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.soma369.laimory.core.ui.R as UiR

@Composable
internal fun TimelineEventCard(
    event: TimelineEventUiModel,
    onEditClick: () -> Unit,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
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
                    )
                } else {
                    EventMainCard(event = event, onEditClick = onEditClick)
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
                    TimelineMemo(memo = event.memo)
                }
            }
        }
    }
}

@Composable
private fun EventMainCard(
    event: TimelineEventUiModel,
    onEditClick: () -> Unit,
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
            TimelineEditButton(onClick = onEditClick)
        }
    }
}

@Composable
private fun PhotoMainEvent(
    event: TimelineEventUiModel,
    photoSlots: List<String?>,
    onEditClick: () -> Unit,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
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
                        modifier =
                            Modifier
                                .width(photoWidth)
                                .height(92.dp),
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
            TimelineEditButton(onClick = onEditClick)
        }
        TimelineMemo(memo = event.memo)
    }
}

@Composable
private fun PhotoThumbnailRow(
    photoSlots: List<String?>,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val visiblePhotos = photoSlots.take(THUMBNAIL_PHOTO_LIMIT)
        visiblePhotos.forEachIndexed { index, photoUrl ->
            TimelinePhoto(
                photoUrl = photoUrl,
                onClick = { onPhotoClick(photoSlots, index) },
                remainingCount =
                    if (index == visiblePhotos.lastIndex) {
                        photoSlots.size - visiblePhotos.size
                    } else {
                        0
                    },
                modifier = Modifier.size(64.dp),
                cornerRadius = 4.dp,
            )
        }
    }
}

@Composable
private fun TimelinePhoto(
    photoUrl: String?,
    remainingCount: Int = 0,
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
        if (remainingCount > 0) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.56f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$remainingCount",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
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

@Composable
private fun TimelineMemo(memo: String?) {
    val borderColor = MaterialTheme.colorScheme.outline
    Text(
        text = memo?.takeIf(String::isNotBlank) ?: "이 순간에 대한 메모…",
        modifier =
            Modifier
                .fillMaxWidth()
                .dashedRoundRectBorder(
                    color = borderColor,
                    cornerRadius = 12.dp,
                ).padding(horizontal = Spacing.medium, vertical = 10.dp),
        style = MaterialTheme.laimorySignature.note,
        color =
            if (memo.isNullOrBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
    )
}

private fun Modifier.dashedRoundRectBorder(
    color: Color,
    cornerRadius: Dp,
) = drawWithCache {
    val strokeWidth = 1.dp.toPx()
    val radius = cornerRadius.toPx()
    val dash = 4.dp.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
    onDrawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            style = Stroke(width = strokeWidth, pathEffect = pathEffect),
        )
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

private const val THUMBNAIL_PHOTO_LIMIT = 3

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

@Preview(name = "사진 여러 장 첨부", showBackground = true, widthDp = 360)
@Composable
private fun PhotoThumbnailEventCardPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = photoThumbnailPreviewEvent(),
                onEditClick = {},
                onPhotoClick = { _, _ -> },
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
