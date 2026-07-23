package com.soma369.laimory.feature.timeline.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.timeline.component.TimelineDeleteDialog
import com.soma369.laimory.feature.timeline.component.TimelineEditorTextSection
import com.soma369.laimory.feature.timeline.component.TimelineEventPhotoSection
import com.soma369.laimory.feature.timeline.component.TimelineEventTimeSection
import com.soma369.laimory.feature.timeline.component.TimelineEventTypeSection
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorForm
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiContent
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorUiState
import com.soma369.laimory.feature.timeline.state.TimelineEventEditorValidation
import com.soma369.laimory.feature.timeline.state.TimelineEventPendingPhoto
import com.soma369.laimory.feature.timeline.state.TimelineEventPhotoUploadState
import com.soma369.laimory.feature.timeline.state.TimelineEventTimeField
import com.soma369.laimory.feature.timeline.viewmodel.TimelineEventEditorViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.LocalTime
import com.soma369.laimory.core.ui.R as UiR

@Composable
fun TimelineEventEditorRoute(
    innerPadding: PaddingValues,
    timelineEventId: Long,
    viewModel: TimelineEventEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(timelineEventId) {
        viewModel.sendIntent(TimelineEventEditorUiIntent.Initialize(timelineEventId))
    }
    TimelineEventEditorContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun TimelineEventEditorContent(
    innerPadding: PaddingValues,
    state: TimelineEventEditorUiState,
    onIntent: (TimelineEventEditorUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<TimelineEventEditorUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val listState = rememberLazyListState()
    val titleFocusRequester = remember { FocusRequester() }
    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(),
        ) { uris ->
            onIntent(TimelineEventEditorUiIntent.AddPhotos(uris.map { it.toString() }))
        }

    LaunchedEffect(snackbarFlow) {
        snackbarFlow.collect(snackbarHostState::showSnackbar)
    }
    LaunchedEffect(sideEffectFlow) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                TimelineEventEditorUiSideEffect.LaunchPhotoPicker ->
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                TimelineEventEditorUiSideEffect.FocusTitle -> {
                    listState.animateScrollToItem(TITLE_ITEM_INDEX)
                    titleFocusRequester.requestFocus()
                }
                is TimelineEventEditorUiSideEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    BackHandler(enabled = state.content == TimelineEventEditorUiContent.Editor) {
        onIntent(TimelineEventEditorUiIntent.NavigateBack)
    }

    TimelineEventEditorScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
        listState = listState,
        titleFocusRequester = titleFocusRequester,
    )

    state.editingTimeField?.let { field ->
        state.form?.let { form ->
            TimelineEventTimePickerDialog(
                field = field,
                initialTime =
                    when (field) {
                        TimelineEventTimeField.START -> form.startAt.toLocalTime()
                        TimelineEventTimeField.END -> form.endAt?.toLocalTime() ?: form.startAt.toLocalTime()
                    },
                onConfirm = { onIntent(TimelineEventEditorUiIntent.SelectTime(field, it)) },
                onClearEnd = { onIntent(TimelineEventEditorUiIntent.ClearEndTime) },
                onDismiss = { onIntent(TimelineEventEditorUiIntent.DismissTimePicker) },
            )
        }
    }

    if (state.isDiscardDialogVisible) {
        AlertDialog(
            onDismissRequest = { onIntent(TimelineEventEditorUiIntent.DismissDiscard) },
            title = { Text("변경사항을 버릴까요?") },
            text = { Text("저장하지 않은 내용과 추가할 사진이 사라집니다.") },
            confirmButton = {
                TextButton(onClick = { onIntent(TimelineEventEditorUiIntent.ConfirmDiscard) }) {
                    Text("나가기")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(TimelineEventEditorUiIntent.DismissDiscard) }) {
                    Text("계속 편집")
                }
            },
        )
    }

    TimelineDeleteDialog(
        state = state.deleteDialogState,
        confirmationTitle = "이 이벤트를 삭제할까요?",
        confirmationMessage =
            buildString {
                append("이벤트와 연결된 서버 기록이 함께 삭제됩니다. ")
                append("기기의 원본 사진과 수집 데이터는 삭제되지 않습니다.")
                if (state.hasUnsavedChanges) {
                    append("\n\n저장하지 않은 변경사항도 사라집니다.")
                }
            },
        successMessage = "이벤트를 삭제했습니다.",
        onConfirm = { onIntent(TimelineEventEditorUiIntent.ConfirmDelete) },
        onDismiss = { onIntent(TimelineEventEditorUiIntent.DismissDelete) },
        onFinish = { onIntent(TimelineEventEditorUiIntent.FinishDelete) },
    )
}

@Composable
private fun TimelineEventEditorScreen(
    innerPadding: PaddingValues,
    state: TimelineEventEditorUiState,
    onIntent: (TimelineEventEditorUiIntent) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    titleFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
    ) {
        LaimoryTopAppBar(
            title = { Text("이벤트 수정") },
            onBackClick = { onIntent(TimelineEventEditorUiIntent.NavigateBack) },
            actions = {
                IconButton(
                    onClick = {},
                    enabled = false,
                ) {
                    Icon(
                        painter = painterResource(UiR.drawable.ico_default_more),
                        contentDescription = "이벤트 메뉴",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
        )

        when (state.content) {
            TimelineEventEditorUiContent.Loading -> TimelineEventEditorLoading()
            TimelineEventEditorUiContent.Unavailable ->
                TimelineEventEditorUnavailable(
                    onBackClick = { onIntent(TimelineEventEditorUiIntent.NavigateBack) },
                )
            TimelineEventEditorUiContent.Editor ->
                state.form?.let {
                    TimelineEventEditorBody(
                        state = state,
                        form = it,
                        onIntent = onIntent,
                        listState = listState,
                        titleFocusRequester = titleFocusRequester,
                    )
                }
        }
    }
}

@Composable
private fun TimelineEventEditorBody(
    state: TimelineEventEditorUiState,
    form: TimelineEventEditorForm,
    onIntent: (TimelineEventEditorUiIntent) -> Unit,
    listState: LazyListState,
    titleFocusRequester: FocusRequester,
) {
    val enabled =
        !state.isSaving &&
            !state.isReadOnly &&
            state.deleteDialogState == TimelineDeleteDialogState.Hidden
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentPadding =
                PaddingValues(
                    start = Spacing.large,
                    top = Spacing.small,
                    end = Spacing.large,
                    bottom = Spacing.extraLarge2,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge2),
        ) {
            item {
                TimelineEventTypeSection(
                    selectedType = form.eventType,
                    enabled = enabled,
                    onSelect = { onIntent(TimelineEventEditorUiIntent.ChangeEventType(it)) },
                )
            }
            item {
                TimelineEditorTextSection(
                    title = "제목",
                    value = form.title,
                    onValueChange = { onIntent(TimelineEventEditorUiIntent.ChangeTitle(it)) },
                    enabled = enabled,
                    error = state.validation.titleError,
                    placeholder = "제목을 입력하세요",
                    supportingText = "최대 255자",
                    focusRequester = titleFocusRequester,
                )
            }
            item {
                TimelineEditorTextSection(
                    title = "설명",
                    value = form.subtitle,
                    onValueChange = { onIntent(TimelineEventEditorUiIntent.ChangeSubtitle(it)) },
                    enabled = enabled,
                    error = state.validation.subtitleError,
                    placeholder = "설명을 입력하세요",
                    supportingText = "선택 입력 · 최대 255자",
                )
            }
            item {
                TimelineEventTimeSection(
                    startAt = form.startAt,
                    endAt = form.endAt,
                    activeField = state.editingTimeField,
                    enabled = enabled,
                    error = state.validation.timeError,
                    onStartClick = {
                        onIntent(TimelineEventEditorUiIntent.ShowTimePicker(TimelineEventTimeField.START))
                    },
                    onEndClick = {
                        onIntent(TimelineEventEditorUiIntent.ShowTimePicker(TimelineEventTimeField.END))
                    },
                )
            }
            item {
                TimelineEventPhotoSection(
                    existingPhotoUrls = state.existingPhotoUrls,
                    pendingPhotos = state.pendingPhotos,
                    enabled = enabled,
                    onAddClick = { onIntent(TimelineEventEditorUiIntent.OpenPhotoPicker) },
                    onRemovePending = { onIntent(TimelineEventEditorUiIntent.RemovePendingPhoto(it)) },
                )
            }
            item {
                TimelineEditorTextSection(
                    title = "메모",
                    value = form.memo,
                    onValueChange = { onIntent(TimelineEventEditorUiIntent.ChangeMemo(it)) },
                    enabled = enabled,
                    error = state.validation.memoError,
                    placeholder = "메모를 입력하세요",
                    supportingText = "선택 입력",
                    singleLine = false,
                    counter = "${form.memo.length}/10,000",
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.large,
                        top = Spacing.medium,
                        end = Spacing.large,
                        bottom = Spacing.large,
                    ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            OutlinedButton(
                onClick = { onIntent(TimelineEventEditorUiIntent.RequestDelete) },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(ActionButtonHeight),
                enabled = enabled,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = "삭제",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { onIntent(TimelineEventEditorUiIntent.Save) },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(ActionButtonHeight),
                enabled = state.isSaveEnabled,
                shape = MaterialTheme.shapes.medium,
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "저장",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimelineEventTimePickerDialog(
    field: TimelineEventTimeField,
    initialTime: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onClearEnd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (field == TimelineEventTimeField.START) {
                    "시작 시각"
                } else {
                    "종료 시각"
                },
            )
        },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(pickerState.hour, pickerState.minute))
                },
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            Row {
                if (field == TimelineEventTimeField.END) {
                    TextButton(onClick = onClearEnd) {
                        Text("종료 없음")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        },
    )
}

@Composable
private fun TimelineEventEditorLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun TimelineEventEditorUnavailable(onBackClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "수정할 이벤트를 찾을 수 없어요",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "타임라인으로 돌아가 최신 기록을 확인해 주세요.",
            modifier = Modifier.padding(top = Spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.padding(top = Spacing.extraLarge),
        ) {
            Text("돌아가기")
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelineEventEditorPreview() {
    LaimoryTheme {
        TimelineEventEditorScreen(
            innerPadding = PaddingValues(),
            state = previewEditorState(),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelineEventEditorSavingPreview() {
    LaimoryTheme {
        TimelineEventEditorScreen(
            innerPadding = PaddingValues(),
            state = previewEditorState().copy(isSaving = true),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelineEventEditorValidationPreview() {
    LaimoryTheme {
        TimelineEventEditorScreen(
            innerPadding = PaddingValues(),
            state =
                previewEditorState().copy(
                    validation =
                        TimelineEventEditorValidation(
                            titleError = "제목을 입력해 주세요.",
                            timeError = "종료 시각은 시작 시각보다 빠를 수 없어요.",
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun TimelineEventEditorDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        TimelineEventEditorScreen(
            innerPadding = PaddingValues(),
            state = previewEditorState(),
            onIntent = {},
        )
    }
}

private fun previewEditorState(): TimelineEventEditorUiState {
    val form =
        TimelineEventEditorForm(
            eventType = TimelineEventType.MOVEMENT,
            title = "출근길",
            subtitle = "강남역 → 성수역 · 7호선",
            startAt = LocalDateTime.of(2026, 5, 8, 8, 30),
            endAt = LocalDateTime.of(2026, 5, 8, 9, 0),
            memo = "7호선이 평소보다 많이 붐볐다.",
        )
    return TimelineEventEditorUiState(
        timelineEventId = 17L,
        content = TimelineEventEditorUiContent.Editor,
        originalForm = form,
        form = form,
        existingPhotoUrls = listOf("preview://photo-1", "preview://photo-2"),
        pendingPhotos =
            listOf(
                TimelineEventPendingPhoto(
                    rawId = "pending-photo",
                    clientPhotoUri = "preview://pending",
                    uploadState = TimelineEventPhotoUploadState.PENDING,
                ),
            ),
    )
}

private val ActionButtonHeight = 44.dp
private const val TITLE_ITEM_INDEX = 1
