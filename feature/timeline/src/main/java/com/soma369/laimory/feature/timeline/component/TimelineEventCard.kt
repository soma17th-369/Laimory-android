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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
    isLast: Boolean = false,
    memoEditor: TimelineMemoEditorState? = null,
    onMemoClick: () -> Unit = {},
    onMemoChange: (String) -> Unit = {},
    onMemoCancel: () -> Unit = {},
    onMemoConfirm: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 읽기 모드는 편집 도구가 없는 대신 시안의 타임라인 행으로 보여 준다. 편집 모드 표현은
    // 이번 범위에서 바꾸지 않는다 — 두 모드가 같은 레이아웃을 쓰면 편집 버튼 자리 때문에
    // 읽기 화면까지 카드로 남는다.
    if (isEditable) {
        EditModeEventCard(
            event = event,
            onEditClick = onEditClick,
            onPhotoClick = onPhotoClick,
            memoEditor = memoEditor,
            onMemoClick = onMemoClick,
            onMemoChange = onMemoChange,
            onMemoCancel = onMemoCancel,
            onMemoConfirm = onMemoConfirm,
            modifier = modifier,
        )
    } else {
        ReadModeEventRow(
            event = event,
            isLast = isLast,
            onPhotoClick = onPhotoClick,
            memoEditor = memoEditor,
            onMemoClick = onMemoClick,
            onMemoChange = onMemoChange,
            onMemoCancel = onMemoCancel,
            onMemoConfirm = onMemoConfirm,
            modifier = modifier,
        )
    }
}

/**
 * 읽기 모드 타임라인 행.
 *
 * 왼쪽 표시자 열(아이콘 + 연결선)과 오른쪽 본문 열로 나뉜다. 본문은 세 가지로 배치된다.
 * - 기본: 시각 → 제목 → 부제 → 메모
 * - 사진이 딸린 이벤트: 시각 → 제목 → 부제 → 사진 → 메모
 * - 사진이 본문인 이벤트: 시각 → 사진 → 메모 (제목·부제 없음)
 *
 * 값이 없는 줄은 그리지 않는다 — 빈 자리를 남기면 행마다 높이가 달라 목록이 들쭉날쭉해진다.
 */
@Composable
private fun ReadModeEventRow(
    event: TimelineEventUiModel,
    isLast: Boolean,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
    memoEditor: TimelineMemoEditorState?,
    onMemoClick: () -> Unit,
    onMemoChange: (String) -> Unit,
    onMemoCancel: () -> Unit,
    onMemoConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photoSlots = event.photoSlots()
    val isPhotoMain = event.eventType == TimelineEventType.PHOTO_MOMENT && photoSlots.isNotEmpty()
    val connectorColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    // 연결선은 행 높이만큼 늘어나야 해서 IntrinsicSize 로 잡고 싶지만, 사진 목록이
                    // LazyRow 라 intrinsic 측정을 지원하지 않는다. 그래서 배경으로 직접 그린다.
                    if (isLast) return@drawBehind
                    val center = INDICATOR_SIZE.toPx() / 2
                    val top = INDICATOR_SIZE.toPx()
                    if (size.height <= top) return@drawBehind
                    drawLine(
                        color = connectorColor,
                        start = Offset(center, top),
                        end = Offset(center, size.height),
                        strokeWidth = CONNECTOR_WIDTH.toPx(),
                        cap = StrokeCap.Round,
                    )
                },
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        EventTypeIndicator(eventType = event.eventType)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            // 시각 줄은 아이콘과 같은 높이로 잡아 첫 줄이 아이콘 가운데에 맞물린다.
            Box(
                modifier = Modifier.height(INDICATOR_SIZE),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = event.emphasizedTimeLabel(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (!isPhotoMain) {
                Text(
                    text = event.title.ifBlank { event.eventType.label() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                event.subtitle?.takeIf(String::isNotBlank)?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (photoSlots.isNotEmpty()) {
                ReadModePhotos(
                    photoSlots = photoSlots,
                    isPhotoMain = isPhotoMain,
                    onPhotoClick = onPhotoClick,
                )
            }
            TimelineMemo(
                memo = event.memo,
                question = event.question,
                editor = memoEditor,
                isEditable = false,
                onClick = onMemoClick,
                onValueChange = onMemoChange,
                onCancel = onMemoCancel,
                onConfirm = onMemoConfirm,
            )
        }
    }
}

/**
 * 표시자 아이콘. 연결선은 행 배경이 그리므로 여기서는 원형 아이콘만 둔다.
 *
 * 채움색은 시안의 `surface-variant` 대신 화면 배경색을 쓴다 — 읽기 화면은 배경 하나로 이어지는
 * 면이라, 행마다 밝기가 다른 원이 찍히면 아이콘이 아니라 원이 먼저 눈에 띈다. 원형 자리는 그대로
 * 남긴다: 24dp 아이콘을 32dp 안에 가운데 두고 연결선의 시작점을 잡아 주는 배치 기준이다.
 */
@Composable
private fun EventTypeIndicator(eventType: TimelineEventType) {
    Box(
        modifier =
            Modifier
                .size(INDICATOR_SIZE)
                .clip(RoundedCornerShape(INDICATOR_CORNER_RADIUS))
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(eventType.iconResource()),
            contentDescription = null,
            modifier = Modifier.size(INDICATOR_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 사진이 본문이면 크게, 딸린 사진이면 썸네일로 보여 준다. */
@Composable
private fun ReadModePhotos(
    photoSlots: List<String?>,
    isPhotoMain: Boolean,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
) {
    if (!isPhotoMain) {
        PhotoThumbnailRow(photoSlots = photoSlots, onPhotoClick = onPhotoClick)
        return
    }
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
}

@Composable
private fun EditModeEventCard(
    event: TimelineEventUiModel,
    onEditClick: () -> Unit,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
    memoEditor: TimelineMemoEditorState?,
    onMemoClick: () -> Unit,
    onMemoChange: (String) -> Unit,
    onMemoCancel: () -> Unit,
    onMemoConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditable = true
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
                        question = event.question,
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
            // 읽기 모드에서도 자리를 비워 둔다 — 버튼 유무로 제목·부제목의 줄 수가 바뀌면
            // 모드를 오갈 때 카드 높이가 변해 목록 전체가 밀린다.
            TimelineEditSlot(isEditable = isEditable, onEditClick = onEditClick)
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
            TimelineEditSlot(isEditable = isEditable, onEditClick = onEditClick)
        }
        TimelineMemo(
            memo = event.memo,
            question = event.question,
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

/**
 * 편집 버튼 자리.
 *
 * 읽기 모드에서는 버튼을 그리지 않되 같은 크기를 차지한다. 자리를 비우면 제목·부제목이 쓸 수 있는
 * 폭이 달라져 한 줄이던 텍스트가 두 줄이 되고, 모드를 오갈 때마다 카드 높이가 튄다.
 */
@Composable
private fun TimelineEditSlot(
    isEditable: Boolean,
    onEditClick: () -> Unit,
) {
    if (isEditable) {
        TimelineEditButton(onClick = onEditClick)
        return
    }
    Spacer(modifier = Modifier.size(TimelineEditButtonSize))
}

@Composable
private fun TimelineEditButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(TimelineEditButtonSize),
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

private val TimelineEditButtonSize = 28.dp

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 시작 시각을 앞세운 시각 표기.
 *
 * 읽기 모드에서 이 줄은 제목(Title/Large)보다 작은 Title/Medium 이라 크기로는 강조되지 않는다.
 * 대신 한 줄 안에서 **색**으로 초점을 만든다 — 사용자가 먼저 읽어야 하는 시작 시각만 본문색으로
 * 두고 종료 구간은 흐린 색으로 낮춘다. 크기를 나누지 않는 이유는 한 줄 안에서 글자 크기가
 * 섞이면 기준선이 흔들려 읽기 나빠지기 때문이다.
 *
 * 종료 시각이 없는 이벤트는 나눌 것이 없어 한 덩어리로 둔다.
 */
@Composable
private fun TimelineEventUiModel.emphasizedTimeLabel(): AnnotatedString {
    val start = startAt.format(TimeFormatter)
    val end = endAt?.format(TimeFormatter)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    return buildAnnotatedString {
        withStyle(SpanStyle(color = onSurface)) { append(start) }
        if (end != null) {
            withStyle(SpanStyle(color = onSurfaceVariant)) { append(" ~ $end") }
        }
    }
}

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

@Preview(name = "읽기 모드 / 이벤트 타입별", showBackground = true, widthDp = 360)
@Composable
private fun ReadModeEventTypePreview(
    @PreviewParameter(TimelineEventPreviewParameterProvider::class)
    event: TimelineEventUiModel,
) {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = event,
                onEditClick = {},
                onPhotoClick = { _, _ -> },
                isEditable = false,
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

/** 연결선이 이어지는 모습과 마지막 행에서 끊기는 모습을 함께 본다. */
@Preview(name = "읽기 모드 / 연결선", showBackground = true, widthDp = 360)
@Composable
private fun ReadModeConnectorPreview() {
    val events = TimelineEventPreviewParameterProvider().values.take(3).toList()
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(Spacing.large),
                verticalArrangement = Arrangement.spacedBy(Spacing.large),
            ) {
                events.forEachIndexed { index, event ->
                    TimelineEventCard(
                        event = event,
                        onEditClick = {},
                        onPhotoClick = { _, _ -> },
                        isEditable = false,
                        isLast = index == events.lastIndex,
                    )
                }
            }
        }
    }
}

@Preview(name = "읽기 모드 / 사진 딸린 이벤트", showBackground = true, widthDp = 360)
@Composable
private fun ReadModePhotoThumbnailPreview(
    @PreviewParameter(PhotoThumbnailCountPreviewParameterProvider::class)
    event: TimelineEventUiModel,
) {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = event,
                onEditClick = {},
                onPhotoClick = { _, _ -> },
                isEditable = false,
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

@Preview(name = "읽기 모드 / 사진 본문 이벤트", showBackground = true, widthDp = 360)
@Composable
private fun ReadModePhotoMainPreview(
    @PreviewParameter(PhotoMainCountPreviewParameterProvider::class)
    event: TimelineEventUiModel,
) {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = event,
                onEditClick = {},
                onPhotoClick = { _, _ -> },
                isEditable = false,
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

@Preview(name = "읽기 모드 / 다크", showBackground = true, widthDp = 360)
@Composable
private fun ReadModeDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = TimelineEventPreviewParameterProvider().values.first(),
                onEditClick = {},
                onPhotoClick = { _, _ -> },
                isEditable = false,
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

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

/**
 * 질문 표시 조합 미리보기.
 *
 * 메모가 있으면 메모가 질문을 가리고, 메모가 비면 질문이 안내 문구 자리를 차지한다.
 * 읽기 모드에서 둘 다 없으면 영역째 사라진다.
 */
@Preview(name = "질문·메모 조합", showBackground = true, widthDp = 360)
@Composable
private fun QuestionPromptCardPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(Spacing.large),
                verticalArrangement = Arrangement.spacedBy(Spacing.large),
            ) {
                TimelineEventCard(
                    event = questionPreviewEvent(memo = null, question = "오늘 누구와 함께였나요?"),
                    onEditClick = {},
                    onPhotoClick = { _, _ -> },
                )
                TimelineEventCard(
                    event = questionPreviewEvent(memo = "혼자 조용히 걸었다.", question = "오늘 누구와 함께였나요?"),
                    onEditClick = {},
                    onPhotoClick = { _, _ -> },
                )
                TimelineEventCard(
                    event = questionPreviewEvent(memo = null, question = null),
                    onEditClick = {},
                    onPhotoClick = { _, _ -> },
                    isEditable = false,
                )
            }
        }
    }
}

private fun questionPreviewEvent(
    memo: String?,
    question: String?,
) = TimelineEventUiModel(
    timelineEventId = 9L,
    eventType = TimelineEventType.MOVEMENT,
    startAt = LocalDateTime.of(2026, 5, 8, 19, 0),
    endAt = null,
    title = "저녁 산책",
    subtitle = "한강공원",
    memo = memo,
    question = question,
    itemCounts = emptyList(),
)

private fun defaultPreviewEvent(eventType: TimelineEventType) =
    TimelineEventUiModel(
        timelineEventId = eventType.ordinal.toLong(),
        eventType = eventType,
        startAt = LocalDateTime.of(2026, 5, 8, 9, 0).plusHours(eventType.ordinal.toLong()),
        endAt = null,
        title = eventType.label(),
        subtitle = "타입별 타임라인 이벤트",
        memo = "이벤트 타입에 맞는 아이콘과 텍스트 스타일을 확인해요.",
        question = null,
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
        question = null,
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
        question = null,
        itemCounts = listOf(TimelineItemCountUiModel(TimelineItemType.PHOTO, photoCount)),
        photoUrls = List(photoCount) { null },
    )

/** 표시자 원형 크기. 시안 32dp. */
private val INDICATOR_SIZE = 32.dp
private val INDICATOR_CORNER_RADIUS = 16.dp
private val INDICATOR_ICON_SIZE = 24.dp

/** 아이콘 아래로 이어지는 연결선 두께. 시안 2dp. */
private val CONNECTOR_WIDTH = 2.dp
