package com.soma369.laimory.feature.home.screen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.component.DateHeaderCard
import com.soma369.laimory.feature.home.component.DraftSettingsSheet
import com.soma369.laimory.feature.home.component.HomeDatePickerDialog
import com.soma369.laimory.feature.home.component.HomeTimePickerDialog
import com.soma369.laimory.feature.home.component.PastRecordCard
import com.soma369.laimory.feature.home.component.PhotoSelectionSheet
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.HomePastRecordsUiState
import com.soma369.laimory.feature.home.state.HomeTimeField
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

    LaunchedEffect(Unit) {
        snackbarFlow.collect(snackbarHostState::showSnackbar)
    }
    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
            when (effect) {
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

    state.editingTimeField?.let { field ->
        HomeTimePickerDialog(
            initial = if (field == HomeTimeField.START) state.startTime else state.endTime,
            onConfirm = { onIntent(HomeUiIntent.SelectTime(field, it)) },
            onDismiss = { onIntent(HomeUiIntent.DismissTimePicker) },
        )
    }
}

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
            Text(
                text = "안녕하세요",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item(key = "todayDraftCard") {
            DateHeaderCard(
                state = state,
                onClick = { onIntent(HomeUiIntent.ShowDatePicker) },
                onActionClick = {
                    onIntent(
                        if (state.draftStatus == DraftCreationStatus.SUCCESS) {
                            HomeUiIntent.ViewDraft
                        } else {
                            HomeUiIntent.OpenDraftSheet
                        },
                    )
                },
                onPhotoClick = { onIntent(HomeUiIntent.OpenPhotoSheet) },
            )
        }

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
                        onClick = { onIntent(HomeUiIntent.SelectPastRecord(record.dailyRecordId)) },
                    )
                }
        }
    }
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
