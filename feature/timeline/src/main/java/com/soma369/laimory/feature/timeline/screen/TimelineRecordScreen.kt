package com.soma369.laimory.feature.timeline.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
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
import com.soma369.laimory.feature.timeline.component.TimelineEmotionSheet
import com.soma369.laimory.feature.timeline.component.TimelineEventCard
import com.soma369.laimory.feature.timeline.component.TimelinePhotoViewerDialog
import com.soma369.laimory.feature.timeline.model.TimelineEventUiModel
import com.soma369.laimory.feature.timeline.model.TimelineItemCountUiModel
import com.soma369.laimory.feature.timeline.model.TimelineRecordUiModel
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineMemoEditorState
import com.soma369.laimory.feature.timeline.state.TimelineRecordMode
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

    // 저장 중 시스템·예측 뒤로가기는 소비만 한다 — 요청이 진행 중인 채 화면을 벗어나면
    // 완료 시점의 뒤늦은 pop이 다른 화면을 닫고 실패 안내도 유실된다.
    BackHandler(enabled = state.memoEditor != null || state.isSavingRecord) {
        if (!state.isSavingRecord) onIntent(TimelineRecordUiIntent.NavigateBack)
    }

    TimelineRecordScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
    )

    state.emotionSheet?.let { emotionSheet ->
        TimelineEmotionSheet(
            state = emotionSheet,
            isSaving = state.isSavingRecord,
            onSelect = { onIntent(TimelineRecordUiIntent.SelectEmotion(it)) },
            onConfirm = { onIntent(TimelineRecordUiIntent.ConfirmEmotion) },
            onDismiss = { onIntent(TimelineRecordUiIntent.DismissEmotionSheet) },
        )
    }

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

    // 하루 기록 삭제와 상태를 나눠 관리한다 — 문구도 되돌릴 대상도 다르다.
    // 성공 다이얼로그는 쓰지 않는다. 세션 갱신으로 카드가 사라지는 걸 그대로 보여준다.
    TimelineDeleteDialog(
        state = state.eventDeleteDialogState,
        confirmationTitle =
            state.eventDeleteTarget
                ?.title
                ?.takeIf(String::isNotBlank)
                ?.let { "'$it' 이벤트를 삭제할까요?" }
                ?: "이 이벤트를 삭제할까요?",
        confirmationMessage = "이벤트에 연결된 사진은 기록에서만 빠지고 기기의 원본은 삭제되지 않습니다.",
        successMessage = "",
        onConfirm = { onIntent(TimelineRecordUiIntent.ConfirmDeleteEvent) },
        onDismiss = { onIntent(TimelineRecordUiIntent.DismissDeleteEvent) },
        onFinish = { onIntent(TimelineRecordUiIntent.DismissDeleteEvent) },
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
                .padding(innerPadding)
                // 여백으로 이미 반영한 시스템 바 inset 을 소비 표시한 뒤 남은 만큼만 IME 여백으로 준다.
                // 소비하지 않으면 IME inset 이 내비게이션 바 영역을 포함하고 있어 같은 영역이 두 번 더해진다.
                //
                // IME 여백은 목록이 아니라 화면 전체가 진다 — 목록만 밀면 그 아래 저장 버튼 자리가
                // 키보드 뒤에 그대로 남아, 목록 끝과 키보드 사이에 버튼 높이만큼 빈칸이 생긴다.
                .consumeWindowInsets(innerPadding)
                .imePadding(),
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
                // 기록 삭제는 내용 편집과 별개인 기록 단위 관리 동작이라 SAVED·읽기 모드에서도 노출한다.
                if (state.content is TimelineRecordUiContent.Record) {
                    TimelineRecordModeAction(
                        mode = state.mode,
                        enabled = state.isModeSwitchable,
                        onIntent = onIntent,
                    )
                    Box {
                        IconButton(
                            onClick = { isRecordMenuExpanded = true },
                            enabled = state.isModeSwitchable,
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
                Column(modifier = Modifier.fillMaxSize()) {
                    TimelineRecordBody(
                        record = content.value,
                        memoEditor = state.memoEditor,
                        mode = state.mode,
                        onEventClick = { onIntent(TimelineRecordUiIntent.SelectEvent(it)) },
                        onEventDeleteClick = { onIntent(TimelineRecordUiIntent.RequestDeleteEvent(it)) },
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
                        modifier = Modifier.weight(1f),
                    )
                    // 메모 편집 중에는 어차피 누를 수 없는 버튼이라 감춘다 — 키보드 위 좁은 자리를
                    // 비활성 버튼이 차지하지 않게 한다.
                    // 저장은 내용 변경이 아니라 상태 확정이라 모드와 무관하게 노출한다. SAVED 는 재호출하지 않는다.
                    if (!content.value.isSaved && state.memoEditor == null) {
                        SaveRecordButton(
                            enabled =
                                !state.isSavingRecord &&
                                    state.emotionSheet == null &&
                                    state.deleteDialogState == TimelineDeleteDialogState.Hidden,
                            isLoading = state.isSavingRecord,
                            onClick = { onIntent(TimelineRecordUiIntent.RequestSave) },
                        )
                    }
                }
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

/**
 * 읽기 모드의 `편집`, 편집 모드의 `X`.
 *
 * `X` 는 저장이 아니라 화면 모드만 닫는다 — 편집 결과는 각 API 성공 시점에 이미 반영돼 있다.
 * 진행 중인 작업이 있으면 비활성으로 두어 눌러도 반응이 없는 상태를 만들지 않는다.
 */
@Composable
private fun TimelineRecordModeAction(
    mode: TimelineRecordMode,
    enabled: Boolean,
    onIntent: (TimelineRecordUiIntent) -> Unit,
) {
    if (mode.isEditing) {
        IconButton(
            onClick = { onIntent(TimelineRecordUiIntent.ExitEditMode) },
            enabled = enabled,
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ico_default_close),
                contentDescription = "편집 닫기",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        return
    }
    TextButton(
        onClick = { onIntent(TimelineRecordUiIntent.EnterEditMode) },
        enabled = enabled,
    ) {
        Text(text = "편집", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TimelineRecordBody(
    record: TimelineRecordUiModel,
    mode: TimelineRecordMode,
    memoEditor: TimelineMemoEditorState?,
    onEventClick: (Long) -> Unit,
    onEventDeleteClick: (Long) -> Unit,
    onMemoClick: (Long) -> Unit,
    onMemoChange: (String) -> Unit,
    onMemoCancel: () -> Unit,
    onMemoConfirm: () -> Unit,
    onPhotoClick: (photoUrls: List<String?>, initialIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (record.events.isEmpty()) {
        TimelineRecordEmpty(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
                onDeleteClick = { onEventDeleteClick(event.timelineEventId) },
                onPhotoClick = onPhotoClick,
                isEditable = mode.isEditing,
                memoEditor = memoEditor?.takeIf { it.timelineEventId == event.timelineEventId },
                onMemoClick = { onMemoClick(event.timelineEventId) },
                onMemoChange = onMemoChange,
                onMemoCancel = onMemoCancel,
                onMemoConfirm = onMemoConfirm,
            )
        }
    }
}

@Composable
private fun SaveRecordButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large, vertical = Spacing.medium)
                .height(52.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = "저장하기",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

private data class TimelinePhotoViewerState(
    val photoUrls: List<String?>,
    val initialIndex: Int,
)

@Composable
private fun TimelineRecordEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
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
            state =
                TimelineRecordUiState(
                    content = TimelineRecordUiContent.Record(previewRecord()),
                    mode = TimelineRecordMode.EDIT,
                ),
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
            state =
                TimelineRecordUiState(
                    content = TimelineRecordUiContent.Record(previewRecord()),
                    mode = TimelineRecordMode.EDIT,
                ),
            onIntent = {},
        )
    }
}

private fun previewRecord() =
    TimelineRecordUiModel(
        dailyRecordId = 31L,
        recordDate = LocalDate.of(2026, 5, 8),
        isSaved = false,
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
                    question = null,
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
                    question = null,
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
                    question = null,
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
                    question = null,
                    itemCounts = emptyList(),
                ),
            ),
    )
