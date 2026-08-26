package com.soma369.laimory.feature.home.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.timepicker.LaimoryTimePickerSheet
import com.soma369.laimory.core.ui.component.timepicker.LaimoryTimePickerValue
import com.soma369.laimory.core.ui.component.timepicker.TimePickerDateOption
import com.soma369.laimory.core.ui.component.timepicker.TimePickerField
import com.soma369.laimory.core.ui.component.timepicker.TimePickerMinuteStep
import com.soma369.laimory.core.ui.greeting.GreetingEmphasis
import com.soma369.laimory.core.ui.greeting.nicknameGreetingSegments
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.util.permission.PhotoPermission
import com.soma369.laimory.feature.home.component.DateHeaderCard
import com.soma369.laimory.feature.home.component.DraftSettingsSheet
import com.soma369.laimory.feature.home.component.HomeDatePickerDialog
import com.soma369.laimory.feature.home.component.PastRecordCard
import com.soma369.laimory.feature.home.component.PhotoSelectionSheet
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomePastRecordsUiState
import com.soma369.laimory.feature.home.state.HomeTimeField
import com.soma369.laimory.feature.home.state.HomeTimeSheetState
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiSideEffect
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.Flow

@Composable
fun HomeRoute(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    // 홈 진입·복귀 시 지난 기록을 서버와 재동기화한다. (중복 요청 방지는 ViewModel이 소유)
    LaunchedEffect(Unit) {
        viewModel.sendIntent(HomeUiIntent.SyncPastRecords)
    }
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        // 동의 화면에서 제출을 마치고 복귀한 경우를 1회 소비한다.
        viewModel.sendIntent(HomeUiIntent.ConsumeDraftConsentResult)
        viewModel.sendIntent(HomeUiIntent.RefreshProfile)
        viewModel.sendIntent(
            HomeUiIntent.RefreshPhotos(
                hasAccess = PhotoPermission.canRead(context),
                limited = PhotoPermission.isLimited(context),
            ),
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun HomeContent(
    innerPadding: PaddingValues,
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<HomeUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val photoPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onIntent(
                HomeUiIntent.ResolvePhotoAccess(
                    granted = PhotoPermission.canRead(context),
                    limited = PhotoPermission.isLimited(context),
                ),
            )
        }

    LaunchedEffect(Unit) {
        snackbarFlow.collect(snackbarHostState::showSnackbar)
    }
    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is HomeUiSideEffect.RequestPhotoAccess ->
                    if (!effect.force && PhotoPermission.canRead(context)) {
                        onIntent(
                            HomeUiIntent.ResolvePhotoAccess(
                                granted = true,
                                limited = PhotoPermission.isLimited(context),
                            ),
                        )
                    } else {
                        photoPermissionLauncher.launch(PhotoPermission.required())
                    }

                is HomeUiSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    HomeScreen(innerPadding = innerPadding, state = state, onIntent = onIntent)

    if (state.isDraftSheetVisible) {
        DraftSettingsSheet(
            state = state,
            onIntent = onIntent,
        )
    }

    if (state.isPhotoSheetVisible) {
        PhotoSelectionSheet(
            state = state,
            onIntent = onIntent,
        )
    }

    if (state.isDatePickerVisible) {
        HomeDatePickerDialog(
            initialDate = state.selectedDate,
            onSelect = { onIntent(HomeUiIntent.SelectDate(it)) },
            onDismiss = { onIntent(HomeUiIntent.DismissDatePicker) },
        )
    }

    state.timeSheet?.let { sheet ->
        LaimoryTimePickerSheet(
            fields = draftTimePickerFields(sheet),
            expandedFieldId = sheet.expandedField?.name,
            onExpandedFieldChange = { id ->
                onIntent(HomeUiIntent.ExpandTimeField(id?.let(HomeTimeField::valueOf)))
            },
            onValueChange = { id, value, _ ->
                onIntent(HomeUiIntent.ChangeSheetTime(HomeTimeField.valueOf(id), value.date, value.time))
            },
            onConfirm = { onIntent(HomeUiIntent.ConfirmTimeSheet) },
            onDismiss = { onIntent(HomeUiIntent.DismissTimePicker) },
            title = "초안 범위 시각",
            confirmEnabled = sheet.isConfirmEnabled,
            supportingText = DRAFT_WINDOW_GUIDE,
        )
    }
}

/**
 * 시작·종료 두 줄을 한 시트에서 다룬다.
 *
 * 시작은 기록 날짜에 고정이라 날짜 열을 두지 않는다. 종료만 당일·익일 두 선택지를 열어, 예전 종료일
 * 칩이 하던 일을 날짜 열이 대신한다.
 */
private fun draftTimePickerFields(sheet: HomeTimeSheetState): List<TimePickerField> =
    listOf(
        TimePickerField(
            id = HomeTimeField.START.name,
            label = "시작 시각",
            value = LaimoryTimePickerValue(date = sheet.recordDate, time = sheet.startTime),
            // 선택지가 하나뿐이라 날짜 롤러는 감춰지고 값에만 날짜가 붙는다 — 시작일 고정 정책을
            // 유지하면서 어느 날의 시각인지 함께 읽힌다.
            dates = listOf(TimePickerDateOption(sheet.recordDate, DraftEndDay.SAME_DAY.label)),
            minuteStep = TimePickerMinuteStep.FIVE,
        ),
        TimePickerField(
            id = HomeTimeField.END.name,
            label = "종료 시각",
            value = LaimoryTimePickerValue.of(sheet.endDateTime),
            dates =
                DraftEndDay.entries.map { endDay ->
                    TimePickerDateOption(
                        date = sheet.recordDate.plusDays(endDay.dayOffset.toLong()),
                        label = endDay.label,
                    )
                },
            minuteStep = TimePickerMinuteStep.FIVE,
            range = sheet.endRange,
        ),
    )

/** 롤러가 이미 범위를 좁히지만, 왜 그 폭인지는 문구로 알려야 알 수 있다. */
private const val DRAFT_WINDOW_GUIDE = "기록 범위는 6시간 이상이어야 하고, 종료는 익일 06:00까지 고를 수 있어요."

@Composable
private fun HomeScreen(
    innerPadding: PaddingValues,
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
) {
    // 인사·오늘 초안 카드·지난 기록을 중첩 스크롤 없이 하나의 lazy list로 구성한다.
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = Spacing.extraLarge, vertical = Spacing.extraLarge2),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        item(key = "greeting") {
            HomeGreeting(nickname = state.nickname)
        }

        item(key = "todayDraftCard") {
            DateHeaderCard(
                state = state,
                onClick = { onIntent(HomeUiIntent.ShowDatePicker) },
                onActionClick = {
                    onIntent(
                        when (state.draftStatus) {
                            DraftCreationStatus.SUCCESS -> HomeUiIntent.ViewDraft
                            // 생성 중에는 같은 작업의 로딩 화면으로 다시 들어간다.
                            DraftCreationStatus.PROCESSING,
                            DraftCreationStatus.LONG_RUNNING,
                            -> HomeUiIntent.OpenDraftLoading
                            DraftCreationStatus.IDLE, DraftCreationStatus.FAILED -> HomeUiIntent.OpenDraftSheet
                        },
                    )
                },
                onPhotoClick = { onIntent(HomeUiIntent.OpenPhotoSheet) },
            )
        }

        // 수집 실험실은 개발 도구라 release 에서는 진입 버튼 자체를 두지 않는다.
        if (state.isCollectionLabAccessible) {
            item(key = "collectionEntry") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { onIntent(HomeUiIntent.NavigateToCollection) },
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Text("수집 데이터 자세히 보기")
                    }
                }
            }
        }

        item(key = "pastRecordsHeader") {
            Text(
                text = "지난 기록",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (val pastRecords = state.pastRecords) {
            HomePastRecordsUiState.Loading ->
                item(key = "pastRecordsLoading") { PastRecordsLoading() }

            HomePastRecordsUiState.Empty ->
                item(key = "pastRecordsEmpty") { PastRecordsEmpty() }

            HomePastRecordsUiState.LoadFailed ->
                item(key = "pastRecordsLoadFailed") {
                    PastRecordsLoadFailed(onRetryClick = { onIntent(HomeUiIntent.SyncPastRecords) })
                }

            is HomePastRecordsUiState.Content ->
                items(
                    items = pastRecords.records,
                    key = { it.dailyRecordId },
                ) { record ->
                    PastRecordCard(
                        record = record,
                        onClick = { onIntent(HomeUiIntent.SelectPastRecord(record.recordDate)) },
                    )
                }
        }
    }
}

/**
 * 홈 인사말.
 *
 * 닉네임 유무와 무관하게 같은 타이포(`titleLarge`)를 쓴다. 조회가 늦게 끝나도 글자 크기가 바뀌지
 * 않아 목록 첫 줄이 튀지 않는다. 강조는 굵기가 아니라 색 대비다(Figma 규격).
 */
@Composable
private fun HomeGreeting(nickname: String?) {
    val normalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val nicknameColor = MaterialTheme.colorScheme.onSurface
    // 조각을 나눠 여러 Text 로 두면 접근성 서비스가 따로 읽으므로 한 문장으로 합친다.
    val greeting =
        remember(nickname, normalColor, nicknameColor) {
            buildAnnotatedString {
                nicknameGreetingSegments(nickname).forEach { segment ->
                    val color =
                        when (segment.emphasis) {
                            GreetingEmphasis.NORMAL -> normalColor
                            GreetingEmphasis.NICKNAME -> nicknameColor
                        }
                    withStyle(SpanStyle(color = color)) { append(segment.text) }
                }
            }
        }
    Text(text = greeting, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun PastRecordsLoading() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge2),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PastRecordsEmpty() {
    Text(
        text = "아직 저장된 기록이 없어요.\n초안을 만들어 하루를 기록해보세요.",
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PastRecordsLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "지난 기록을 불러오지 못했어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onRetryClick,
            modifier = Modifier.padding(top = Spacing.medium),
        ) {
            Text("다시 시도")
        }
    }
}
