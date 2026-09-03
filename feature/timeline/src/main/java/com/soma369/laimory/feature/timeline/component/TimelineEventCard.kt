package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
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

/**
 * 타임라인 이벤트 한 행.
 *
 * 왼쪽 표시자 열(아이콘 + 연결선)과 오른쪽 본문 열로 나뉘고, 본문은 늘 같은 순서다.
 *
 * ```
 * 시각 [연필·휴지통]   ← 편집 모드에서만 오른쪽에 버튼이 붙는다
 * 제목
 * 부제
 * 사진
 * 메모
 * ```
 *
 * **두 모드가 같은 골격을 쓴다.** 모드마다 다른 배치를 두면 편집이 읽던 화면과 다른 화면이 돼,
 * 지금 손보는 것이 저장 뒤 무엇으로 보일지 알 수 없다. 버튼은 32dp 시각 줄 안에 들어가므로
 * 있고 없고가 제목·부제의 폭을 바꾸지 않는다.
 *
 * 값이 없는 줄은 그리지 않는다 — 빈 자리를 남기면 행마다 높이가 달라 목록이 들쭉날쭉해진다.
 * 메모는 읽기 모드에서 사용자가 남긴 것만 그리므로(`timelineMemoDisplay`), 메모 없는 이벤트는
 * 읽기 쪽이 그만큼 짧다. 쓸 수 없는 자리에 안내 문구를 남기지 않기 위해 받아들인 차이다.
 */
@Composable
internal fun TimelineEventCard(
    event: TimelineEventUiModel,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
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
                }
                // 행 사이 여백을 drawBehind **뒤에** 둔다. 앞에 두면 그리기 영역이 여백을 뺀
                // 크기가 돼 선이 매 행마다 끊긴다. 뒤에 두면 size.height 가 여백까지 포함해
                // 다음 표시자 윗변까지 이어진다.
                .padding(bottom = rowGap(isLast)),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        EventTypeIndicator(eventType = event.eventType)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            EventTimeRow(
                event = event,
                isEditable = isEditable,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
            )
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
            if (photoSlots.isNotEmpty()) {
                EventPhotos(
                    photoSlots = photoSlots,
                    isPhotoMain = isPhotoMain,
                    onPhotoClick = onPhotoClick,
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

/**
 * 시각 줄. 편집 모드에서는 오른쪽 끝에 편집·삭제 버튼이 붙는다.
 *
 * 줄 높이를 표시자와 같은 32dp 로 잡아 첫 줄이 아이콘 가운데에 맞물린다. 최대 높이는 묶지
 * 않는다 — 글꼴을 키운 사용자의 한 줄이 32dp 를 넘으면 시각이 잘린다.
 */
@Composable
private fun EventTimeRow(
    event: TimelineEventUiModel,
    isEditable: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = INDICATOR_SIZE),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = event.emphasizedTimeLabel(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        if (isEditable) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                EventActionButton(
                    iconRes = UiR.drawable.ico_timeline_tool_edit,
                    contentDescription = "이벤트 편집",
                    onClick = onEditClick,
                )
                EventActionButton(
                    iconRes = UiR.drawable.ico_timeline_tool_delete,
                    contentDescription = "이벤트 삭제",
                    onClick = onDeleteClick,
                )
            }
        }
    }
}

/**
 * 시각 줄의 편집·삭제 버튼.
 *
 * 삭제도 편집과 같은 `surfaceVariant` 바탕이다. 시안의 `Mode=Delete` 변형은 바깥 프레임의 흰
 * 채움이 꺼진 채 같은 바탕의 버튼을 감싸고 있어, 겉으로는 두 버튼이 같은 색이다.
 */
@Composable
private fun EventActionButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(ACTION_BUTTON_SIZE),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(ACTION_ICON_SIZE),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 행 아래에 둘 여백. 마지막 행은 이어질 곳이 없으므로 0 이다.
 *
 * 목록의 `Arrangement.spacedBy` 대신 항목이 스스로 진다 — 항목 바깥 간격은 연결선의 그리기
 * 영역에 들어오지 않아 행마다 선이 끊긴다.
 */
private fun rowGap(isLast: Boolean): Dp = if (isLast) 0.dp else TIMELINE_ROW_GAP

/**
 * 표시자 아이콘. 연결선은 행 배경이 그리므로 여기서는 원형 아이콘만 둔다.
 *
 * 채움은 시안대로 `surfaceVariant` 다. 배경색으로 낮춰 봤더니 원이 사라지면서 아이콘이 허공에
 * 뜨고, 아래에서 올라온 연결선도 닿을 곳을 잃어 어색했다. 원은 아이콘의 바탕이자 연결선이
 * 맞물리는 마디라 배경과 구분돼야 한다.
 */
@Composable
private fun EventTypeIndicator(eventType: TimelineEventType) {
    Box(
        modifier =
            Modifier
                .size(INDICATOR_SIZE)
                .clip(RoundedCornerShape(INDICATOR_CORNER_RADIUS))
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
private fun EventPhotos(
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
                    cornerRadius = MAIN_PHOTO_CORNER_RADIUS,
                )
            }
        }
    }
}

@Composable
private fun PhotoThumbnailRow(
    photoSlots: List<String?>,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        itemsIndexed(photoSlots) { index, photoUrl ->
            TimelinePhoto(
                photoUrl = photoUrl,
                onClick = { onPhotoClick(photoSlots, index) },
                modifier = Modifier.size(THUMBNAIL_SIZE),
                cornerRadius = THUMBNAIL_CORNER_RADIUS,
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

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 시작 시각을 앞세운 시각 표기.
 *
 * 이 줄은 제목(Title/Large)보다 작은 Title/Medium 이라 크기로는 강조되지 않는다. 대신 한 줄
 * 안에서 **색**으로 초점을 만든다 — 사용자가 먼저 읽어야 하는 시작 시각만 본문색으로 두고 종료
 * 구간은 흐린 색으로 낮춘다. 크기를 나누지 않는 이유는 한 줄 안에서 글자 크기가 섞이면 기준선이
 * 흔들려 읽기 나빠지기 때문이다.
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

private fun TimelineEventUiModel.photoSlots(): List<String?> {
    val photoCount = itemCounts.firstOrNull { it.itemType == TimelineItemType.PHOTO }?.count ?: 0
    return List(photoCount) { index -> photoUrls.getOrNull(index) }
}

internal fun TimelineEventType.label(): String = displayLabel()

@Preview(name = "편집 모드 / 이벤트 타입별", showBackground = true, widthDp = 360)
@Composable
private fun EditModeEventTypePreview(
    @PreviewParameter(TimelineEventPreviewParameterProvider::class)
    event: TimelineEventUiModel,
) {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = event,
                onEditClick = {},
                onDeleteClick = {},
                onPhotoClick = { _, _ -> },
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

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
                onDeleteClick = {},
                onPhotoClick = { _, _ -> },
                isEditable = false,
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

/**
 * 시안의 `Mode × Photo` 6변형.
 *
 * 같은 이벤트를 두 모드로 나란히 두어, 버튼이 붙고 빠져도 제목·부제·사진 자리가 그대로인지 본다.
 */
@Preview(name = "모드 × 사진 변형", showBackground = true, widthDp = 360, heightDp = 1400)
@Composable
private fun ModePhotoVariantPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(Spacing.large),
                verticalArrangement = Arrangement.spacedBy(Spacing.large),
            ) {
                listOf(
                    photoThumbnailPreviewEvent(photoCount = 0),
                    photoThumbnailPreviewEvent(photoCount = 3),
                    photoMainPreviewEvent(photoCount = 2),
                ).forEach { event ->
                    listOf(true, false).forEach { isEditable ->
                        TimelineEventCard(
                            event = event,
                            onEditClick = {},
                            onDeleteClick = {},
                            onPhotoClick = { _, _ -> },
                            isEditable = isEditable,
                            isLast = true,
                        )
                    }
                }
            }
        }
    }
}

/** 연결선이 이어지는 모습과 마지막 행에서 끊기는 모습을 함께 본다. */
@Preview(name = "연결선", showBackground = true, widthDp = 360)
@Composable
private fun ConnectorPreview() {
    val events = TimelineEventPreviewParameterProvider().values.take(3).toList()
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            // 간격을 주지 않는다 — 실제 목록과 같이 항목이 자기 아래 여백을 지고, 그 여백까지
            // 연결선이 그려지는지 보는 Preview 다. 여기서 더하면 검증 대상이 사라진다.
            Column(modifier = Modifier.padding(Spacing.large)) {
                events.forEachIndexed { index, event ->
                    TimelineEventCard(
                        event = event,
                        onEditClick = {},
                        onDeleteClick = {},
                        onPhotoClick = { _, _ -> },
                        isEditable = false,
                        isLast = index == events.lastIndex,
                    )
                }
            }
        }
    }
}

@Preview(name = "사진 딸린 이벤트", showBackground = true, widthDp = 360)
@Composable
private fun PhotoThumbnailPreview(
    @PreviewParameter(PhotoThumbnailCountPreviewParameterProvider::class)
    photoCount: Int,
) {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = photoThumbnailPreviewEvent(photoCount),
                onEditClick = {},
                onDeleteClick = {},
                onPhotoClick = { _, _ -> },
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

@Preview(name = "사진 본문 이벤트", showBackground = true, widthDp = 360)
@Composable
private fun PhotoMainPreview(
    @PreviewParameter(PhotoMainCountPreviewParameterProvider::class)
    photoCount: Int,
) {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = photoMainPreviewEvent(photoCount),
                onEditClick = {},
                onDeleteClick = {},
                onPhotoClick = { _, _ -> },
                modifier = Modifier.padding(Spacing.large),
            )
        }
    }
}

@Preview(name = "다크", showBackground = true, widthDp = 360)
@Composable
private fun DarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimelineEventCard(
                event = TimelineEventPreviewParameterProvider().values.first(),
                onEditClick = {},
                onDeleteClick = {},
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
                onDeleteClick = {},
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
 * 읽기 모드에서 메모가 없으면 영역째 사라진다.
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
                    onDeleteClick = {},
                    onPhotoClick = { _, _ -> },
                )
                TimelineEventCard(
                    event = questionPreviewEvent(memo = "혼자 조용히 걸었다.", question = "오늘 누구와 함께였나요?"),
                    onEditClick = {},
                    onDeleteClick = {},
                    onPhotoClick = { _, _ -> },
                )
                TimelineEventCard(
                    event = questionPreviewEvent(memo = null, question = "오늘 누구와 함께였나요?"),
                    onEditClick = {},
                    onDeleteClick = {},
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

/**
 * 본문에 딸린 사진의 한 변. 읽기·편집 모드가 같은 값을 쓴다.
 *
 * 종전 64dp 는 20sp 메모·20sp 제목 옆에서 사진이 각주처럼 작아 보였다. 콘텐츠 폭(기기 실측
 * 335dp)에 8dp 간격으로 세 장이 들어가는 크기라, 흔한 1~3장은 스크롤 없이 한눈에 들어오고
 * 그 이상은 가로로 이어진다.
 *
 * 모드별로 나누지 않는 이유는 편집이 읽기 화면의 같은 사진을 손보는 자리이기 때문이다 — 크기가
 * 달라지면 편집 중 보는 것과 저장 뒤 보이는 것이 어긋난다.
 */
private val THUMBNAIL_SIZE = 96.dp
private val THUMBNAIL_CORNER_RADIUS = 8.dp

/** 사진이 본문인 이벤트의 큰 사진. 시안이 콘텐츠 폭에 두 장을 12 간격으로 둔다. */
private val MAIN_PHOTO_CORNER_RADIUS = 12.dp

/** 타임라인 행 사이 간격. */
private val TIMELINE_ROW_GAP = 16.dp

/** 표시자 원형 크기. 시안 32dp. */
private val INDICATOR_SIZE = 32.dp
private val INDICATOR_CORNER_RADIUS = 16.dp
private val INDICATOR_ICON_SIZE = 24.dp

/** 아이콘 아래로 이어지는 연결선 두께. 시안 2dp. */
private val CONNECTOR_WIDTH = 2.dp

/** 시각 줄 오른쪽 버튼. 시안 28dp, 아이콘 24dp. */
private val ACTION_BUTTON_SIZE = 28.dp
private val ACTION_ICON_SIZE = 24.dp
