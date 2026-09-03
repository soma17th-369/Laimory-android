package com.soma369.laimory.feature.timeline.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.EmotionIcon
import com.soma369.laimory.core.ui.component.LaimoryDropdownMenu
import com.soma369.laimory.core.ui.component.LaimoryDropdownMenuItem
import com.soma369.laimory.core.ui.component.LaimoryDropdownMenuItemStyle
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.model.displayLabel
import com.soma369.laimory.core.ui.model.toUiEmotionOrNull
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.timeline.component.TimelineDeleteDialog
import com.soma369.laimory.feature.timeline.component.TimelineEmotionSheet
import com.soma369.laimory.feature.timeline.component.TimelineEventCard
import com.soma369.laimory.feature.timeline.component.TimelineEventDeleteDialog
import com.soma369.laimory.feature.timeline.component.TimelinePhotoViewerDialog
import com.soma369.laimory.feature.timeline.model.TimelineEventUiModel
import com.soma369.laimory.feature.timeline.model.TimelineItemCountUiModel
import com.soma369.laimory.feature.timeline.model.TimelineRecordUiModel
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineEventDeleteDialogState
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

    TimelineEventDeleteDialog(
        state = state.eventDeleteDialogState,
        onConfirm = { onIntent(TimelineRecordUiIntent.ConfirmEventDelete) },
        onDismiss = { onIntent(TimelineRecordUiIntent.DismissEventDelete) },
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
                val record = (state.content as? TimelineRecordUiContent.Record)?.value
                TimelineRecordTitle(
                    title = state.title(),
                    // 저장된 기록에만 자리를 준다. 저장 전에는 감정이라는 것이 아직 없어서,
                    // 자리를 비워 두면 날짜가 가운데에서 밀린다.
                    showsEmotion = record?.isSaved == true,
                    emotion = record?.emotion,
                    isEditable = state.mode.isEditing && state.isModeSwitchable,
                    onEmotionClick = { onIntent(TimelineRecordUiIntent.EditEmotion) },
                )
            },
            onBackClick = { onIntent(TimelineRecordUiIntent.NavigateBack) },
            actions = {
                // 상단 우측은 슬롯 하나다. 돌아갈 읽기 모드가 있을 때만 닫기(X)를 띄우고, 그 밖에는 ⋮ 메뉴다.
                // DRAFT 는 편집이 기본이라 나갈 곳이 없으므로 ⋮ 가 계속 남는다 — 기록 삭제 진입점도 함께 유지된다.
                val record = (state.content as? TimelineRecordUiContent.Record)?.value
                if (record != null) {
                    if (state.mode.isEditing && record.isSaved) {
                        IconButton(
                            onClick = { onIntent(TimelineRecordUiIntent.ExitEditMode) },
                            enabled = state.isModeSwitchable,
                        ) {
                            Icon(
                                painter = painterResource(UiR.drawable.ico_default_close),
                                contentDescription = "편집 닫기",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    } else {
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
                                // 이미 편집 중이면 진입 항목을 띄우지 않는다(DRAFT 기본 상태).
                                if (!state.mode.isEditing) {
                                    LaimoryDropdownMenuItem(
                                        label = "편집하기",
                                        leadingIcon = painterResource(UiR.drawable.ico_timeline_tool_edit),
                                        onClick = {
                                            isRecordMenuExpanded = false
                                            onIntent(TimelineRecordUiIntent.EnterEditMode)
                                        },
                                    )
                                }
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
                    Box(modifier = Modifier.weight(1f)) {
                        TimelineRecordBody(
                            record = content.value,
                            memoEditor = state.memoEditor,
                            mode = state.mode,
                            onEventClick = { onIntent(TimelineRecordUiIntent.SelectEvent(it)) },
                            onEventDeleteClick = { onIntent(TimelineRecordUiIntent.RequestEventDelete(it)) },
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
                            modifier = Modifier.fillMaxSize(),
                        )
                        // 목록 위에 얹어 저장 버튼과 겹치지 않게 둔다. 편집 모드에서만 보인다 —
                        // 읽는 화면에 만들기 버튼이 떠 있으면 무엇을 읽는 화면인지 흐려진다.
                        if (state.mode.isEditing && state.memoEditor == null) {
                            AddEventFab(
                                enabled = state.isModeSwitchable,
                                onClick = { onIntent(TimelineRecordUiIntent.AddEvent) },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(FabMargin),
                            )
                        }
                    }
                    // 메모 편집 중에는 어차피 누를 수 없는 버튼이라 감춘다 — 키보드 위 좁은 자리를
                    // 비활성 버튼이 차지하지 않게 한다.
                    // 저장은 내용 변경이 아니라 상태 확정이라 모드와 무관하게 노출한다. SAVED 는 재호출하지 않는다.
                    if (!content.value.isSaved && state.memoEditor == null) {
                        SaveRecordButton(
                            enabled =
                                !state.isSavingRecord &&
                                    state.emotionSheet == null &&
                                    state.deleteDialogState == TimelineDeleteDialogState.Hidden &&
                                    state.eventDeleteDialogState == TimelineEventDeleteDialogState.Hidden,
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
        // 행 사이 간격은 항목이 스스로 진다. 여기서 주면 읽기 모드 연결선의 그리기 영역 밖이라
        // 이벤트마다 선이 끊긴다.
    ) {
        itemsIndexed(
            items = record.events,
            key = { _, event -> event.timelineEventId },
        ) { index, event ->
            TimelineEventCard(
                event = event,
                onEditClick = { onEventClick(event.timelineEventId) },
                onDeleteClick = { onEventDeleteClick(event.timelineEventId) },
                onPhotoClick = onPhotoClick,
                isEditable = mode.isEditing,
                isLast = index == record.events.lastIndex,
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

/** 새 이벤트 만들기. 시안의 우하단 플로팅 버튼이다. */
@Composable
private fun AddEventFab(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
    ) {
        // 시안이 아이콘이 아니라 `+` 글자다. 전용 에셋을 만들 만큼 다른 모양이 아니라 그대로 쓴다.
        Text(
            text = "+",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { contentDescription = "이벤트 추가" },
        )
    }
}

private val FabMargin = 20.dp

/**
 * 앱바 타이틀 — 날짜와 하루 감정.
 *
 * 감정은 읽기·편집 모드 모두에 보이고 **편집 모드에서만 누를 수 있다.** 지금까지 감정은 홈 카드와
 * 월간 캘린더에만 있어서, 정작 그 기록을 열면 자신이 무엇을 골랐는지 알 수 없었다.
 *
 * 저장 전(DRAFT)에는 자리째 그리지 않는다. 빈 자리를 남기면 날짜가 가운데에서 밀리고, 저장
 * 전후로 타이틀이 좌우로 움직인다.
 *
 * **저장됐는데 감정이 아직 없는 기록**은 감추지 않고 미상(`?`)으로 그린다 — 감정이 생기기 전에
 * 저장된 기록이 그렇고, 감추면 그 기록만 영영 감정을 넣을 수 없다. 홈·캘린더가 같은 기록을
 * 이미 `?` 로 보여 주고 있어 표시도 어긋나지 않는다.
 */
@Composable
private fun TimelineRecordTitle(
    title: String,
    showsEmotion: Boolean,
    emotion: TimelineEmotion?,
    isEditable: Boolean,
    onEmotionClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, maxLines = 1)
        if (showsEmotion) {
            val label = emotion?.toUiEmotionOrNull().displayLabel()
            EmotionIcon(
                emotion = emotion?.toUiEmotionOrNull(),
                size = TitleEmotionSize,
                contentDescription = if (isEditable) "하루 감정 $label, 눌러서 바꾸기" else "하루 감정 $label",
                modifier =
                    if (isEditable) {
                        Modifier.clickable(onClick = onEmotionClick)
                    } else {
                        Modifier
                    },
            )
        }
    }
}

/** 타이틀 옆 감정 크기. 시안 24dp. */
private val TitleEmotionSize = 24.dp

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
