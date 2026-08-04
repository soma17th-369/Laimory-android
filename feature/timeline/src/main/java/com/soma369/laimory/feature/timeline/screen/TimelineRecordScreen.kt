package com.soma369.laimory.feature.timeline.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryDropdownMenu
import com.soma369.laimory.core.ui.component.LaimoryDropdownMenuItem
import com.soma369.laimory.core.ui.component.LaimoryDropdownMenuItemStyle
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.timeline.component.TimelineDeleteDialog
import com.soma369.laimory.feature.timeline.component.TimelineEventCard
import com.soma369.laimory.feature.timeline.component.TimelinePhotoViewerDialog
import com.soma369.laimory.feature.timeline.model.TimelineEventUiModel
import com.soma369.laimory.feature.timeline.model.TimelineItemCountUiModel
import com.soma369.laimory.feature.timeline.model.TimelineRecordUiModel
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineMemoEditorState
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiContent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiState
import com.soma369.laimory.feature.timeline.viewmodel.TimelineRecordViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.soma369.laimory.core.ui.R as UiR

@Composable
fun TimelineRecordRoute(
    innerPadding: PaddingValues,
    recordDate: LocalDate?,
    viewModel: TimelineRecordViewModel = hiltViewModel(),
) {
    LaunchedEffect(recordDate) {
        viewModel.sendIntent(TimelineRecordUiIntent.Initialize(recordDate))
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    TimelineRecordContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun TimelineRecordContent(
    innerPadding: PaddingValues,
    state: TimelineRecordUiState,
    onIntent: (TimelineRecordUiIntent) -> Unit,
    sideEffectFlow: Flow<TimelineRecordUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    LaunchedEffect(sideEffectFlow) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is TimelineRecordUiSideEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    TimelineRecordScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
    )

    TimelineDeleteDialog(
        state = state.deleteDialogState,
        confirmationTitle =
            state.deleteTarget
                ?.recordDate
                ?.format(RecordDateFormatter)
                ?.let { "$it 기록을 삭제할까요?" }
                ?: "이 하루 기록을 삭제할까요?",
        confirmationMessage =
            "이 날짜의 모든 이벤트와 서버 업로드 사진이 삭제됩니다. " +
                "기기의 원본 사진과 수집 데이터는 삭제되지 않습니다.",
        successMessage = "하루 기록을 삭제했습니다. 홈에서 새 초안을 만들 수 있어요.",
        onConfirm = { onIntent(TimelineRecordUiIntent.ConfirmDelete) },
        onDismiss = { onIntent(TimelineRecordUiIntent.DismissDelete) },
        onFinish = { onIntent(TimelineRecordUiIntent.FinishDelete) },
    )
}

@Composable
private fun TimelineRecordScreen(
    innerPadding: PaddingValues,
    state: TimelineRecordUiState,
    onIntent: (TimelineRecordUiIntent) -> Unit,
) {
    var photoViewerState by remember { mutableStateOf<TimelinePhotoViewerState?>(null) }
    var isRecordMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
    ) {
        LaimoryTopAppBar(
            title = {
                Text(
                    text = state.title(),
                    maxLines = 1,
                )
            },
            onBackClick = { onIntent(TimelineRecordUiIntent.NavigateBack) },
            actions = {
                if (state.content is TimelineRecordUiContent.Record) {
                    Box {
                        IconButton(
                            onClick = { isRecordMenuExpanded = true },
                            enabled =
                                state.deleteDialogState == TimelineDeleteDialogState.Hidden &&
                                    state.memoEditor == null,
                        ) {
                            Icon(
                                painter = painterResource(UiR.drawable.ico_default_more),
                                contentDescription = "기록 메뉴",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        LaimoryDropdownMenu(
                            expanded = isRecordMenuExpanded,
                            onDismissRequest = { isRecordMenuExpanded = false },
                        ) {
                            LaimoryDropdownMenuItem(
                                label = "삭제하기",
                                leadingIcon = painterResource(UiR.drawable.ico_setting_trash),
                                style = LaimoryDropdownMenuItemStyle.Destructive,
                                onClick = {
                                    isRecordMenuExpanded = false
                                    onIntent(TimelineRecordUiIntent.RequestDelete)
                                },
                            )
                        }
                    }
                }
            },
        )

        when (val content = state.content) {
            TimelineRecordUiContent.Loading -> TimelineRecordLoading()
            TimelineRecordUiContent.Unavailable ->
                TimelineRecordUnavailable(
                    onBackClick = { onIntent(TimelineRecordUiIntent.NavigateBack) },
                )
            TimelineRecordUiContent.LoadFailed ->
                TimelineRecordLoadFailed(
                    onRetryClick = { onIntent(TimelineRecordUiIntent.RetryLoad) },
                )
            is TimelineRecordUiContent.Record ->
                TimelineRecordBody(
                    record = content.value,
                    memoEditor = state.memoEditor,
                    onEventClick = { onIntent(TimelineRecordUiIntent.SelectEvent(it)) },
                    onMemoClick = { onIntent(TimelineRecordUiIntent.EditMemo(it)) },
                    onMemoChange = { onIntent(TimelineRecordUiIntent.ChangeMemo(it)) },
                    onMemoCancel = { onIntent(TimelineRecordUiIntent.CancelMemoEdit) },
                    onMemoConfirm = { onIntent(TimelineRecordUiIntent.ConfirmMemoEdit) },
                    onPhotoClick = { photoUrls, initialIndex ->
                        photoViewerState =
                            TimelinePhotoViewerState(
                                photoUrls = photoUrls,
                                initialIndex = initialIndex,
                            )
                    },
                )
        }
    }

    photoViewerState?.let { viewerState ->
        TimelinePhotoViewerDialog(
            photoUrls = viewerState.photoUrls,
            initialIndex = viewerState.initialIndex,
            onDismiss = { photoViewerState = null },
        )
    }
}

@Composable
private fun TimelineRecordLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun TimelineRecordUnavailable(onBackClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "기록을 찾을 수 없어요",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "이미 삭제됐거나 접근할 수 없는 기록이에요.\n홈에서 다른 기록을 선택해주세요.",
            modifier = Modifier.padding(top = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.padding(top = Spacing.extraLarge),
        ) {
            Text("홈으로 돌아가기")
        }
    }
}

@Composable
private fun TimelineRecordLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "기록을 불러오지 못했어요",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "네트워크 상태를 확인한 뒤 다시 시도해주세요.",
            modifier = Modifier.padding(top = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onRetryClick,
            modifier = Modifier.padding(top = Spacing.extraLarge),
        ) {
            Text("다시 시도")
        }
    }
}

@Composable
private fun TimelineRecordBody(
    record: TimelineRecordUiModel,
    memoEditor: TimelineMemoEditorState?,
    onEventClick: (Long) -> Unit,
    onMemoClick: (Long) -> Unit,
    onMemoChange: (String) -> Unit,
    onMemoCancel: () -> Unit,
    onMemoConfirm: () -> Unit,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
) {
    if (record.events.isEmpty()) {
        TimelineRecordEmpty()
        return
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),
        contentPadding = PaddingValues(horizontal = Spacing.large, vertical = Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        itemsIndexed(
            items = record.events,
            key = { _, event -> event.timelineEventId },
        ) { _, event ->
            TimelineEventCard(
                event = event,
                onEditClick = { onEventClick(event.timelineEventId) },
                onPhotoClick = onPhotoClick,
                memoEditor = memoEditor?.takeIf { it.timelineEventId == event.timelineEventId },
                onMemoClick = { onMemoClick(event.timelineEventId) },
                onMemoChange = onMemoChange,
                onMemoCancel = onMemoCancel,
                onMemoConfirm = onMemoConfirm,
            )
        }
    }
}

private data class TimelinePhotoViewerState(
    val photoUrls: List<String?>,
    val initialIndex: Int,
)

@Composable
private fun TimelineRecordEmpty() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.large, vertical = Spacing.small),
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "표시할 타임라인이 없어요",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "이 날짜에는 생성된 이벤트가 없습니다.",
                    modifier = Modifier.padding(top = Spacing.small),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val RecordDateFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

private fun TimelineRecordUiState.title(): String =
    (content as? TimelineRecordUiContent.Record)
        ?.value
        ?.recordDate
        ?.format(RecordDateFormatter)
        ?: "타임라인"

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelineRecordPreview() {
    LaimoryTheme {
        TimelineRecordScreen(
            innerPadding = PaddingValues(),
            state = TimelineRecordUiState(TimelineRecordUiContent.Record(previewRecord())),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelineRecordEmptyPreview() {
    LaimoryTheme {
        TimelineRecordScreen(
            innerPadding = PaddingValues(),
            state =
                TimelineRecordUiState(
                    TimelineRecordUiContent.Record(previewRecord().copy(events = emptyList())),
                ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelineRecordLoadingPreview() {
    LaimoryTheme {
        TimelineRecordScreen(
            innerPadding = PaddingValues(),
            state = TimelineRecordUiState(TimelineRecordUiContent.Loading),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelineRecordUnavailablePreview() {
    LaimoryTheme {
        TimelineRecordScreen(
            innerPadding = PaddingValues(),
            state = TimelineRecordUiState(TimelineRecordUiContent.Unavailable),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelineRecordLoadFailedPreview() {
    LaimoryTheme {
        TimelineRecordScreen(
            innerPadding = PaddingValues(),
            state = TimelineRecordUiState(TimelineRecordUiContent.LoadFailed),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelineRecordDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        TimelineRecordScreen(
            innerPadding = PaddingValues(),
            state = TimelineRecordUiState(TimelineRecordUiContent.Record(previewRecord())),
            onIntent = {},
        )
    }
}

private fun previewRecord() =
    TimelineRecordUiModel(
        dailyRecordId = 31L,
        recordDate = LocalDate.of(2026, 5, 8),
        events =
            listOf(
                TimelineEventUiModel(
                    timelineEventId = 1L,
                    eventType = TimelineEventType.WAKE_UP,
                    startAt = LocalDateTime.of(2026, 5, 8, 7, 30),
                    endAt = LocalDateTime.of(2026, 5, 8, 8, 0),
                    title = "기상 · 준비",
                    subtitle = "집 · 모닝 루틴",
                    memo = null,
                    itemCounts = emptyList(),
                ),
                TimelineEventUiModel(
                    timelineEventId = 2L,
                    eventType = TimelineEventType.MEAL,
                    startAt = LocalDateTime.of(2026, 5, 8, 12, 30),
                    endAt = LocalDateTime.of(2026, 5, 8, 13, 30),
                    title = "점심 · 파스타",
                    subtitle = "성수동 · 트러플 버섯 파스타",
                    memo = "디저트로 크로플도 시켜봤는데 맛있었다.",
                    itemCounts =
                        listOf(
                            TimelineItemCountUiModel(TimelineItemType.PHOTO, 2),
                            TimelineItemCountUiModel(TimelineItemType.STAY, 1),
                        ),
                    photoUrls = listOf(null, null),
                ),
                TimelineEventUiModel(
                    timelineEventId = 3L,
                    eventType = TimelineEventType.PHOTO_MOMENT,
                    startAt = LocalDateTime.of(2026, 5, 8, 18, 20),
                    endAt = null,
                    title = "친구와 카페",
                    subtitle = "성수동 · 작은 카페",
                    memo = "오랜만에 만난 고등학교 친구와 만나서 딸기라떼 먹었다.",
                    itemCounts = listOf(TimelineItemCountUiModel(TimelineItemType.PHOTO, 2)),
                    photoUrls = listOf(null, null),
                ),
                TimelineEventUiModel(
                    timelineEventId = 4L,
                    eventType = TimelineEventType.REST,
                    startAt = LocalDateTime.of(2026, 5, 8, 22, 0),
                    endAt = null,
                    title = "영상 · 휴식",
                    subtitle = "집 · 넷플릭스",
                    memo = null,
                    itemCounts = emptyList(),
                ),
            ),
    )
