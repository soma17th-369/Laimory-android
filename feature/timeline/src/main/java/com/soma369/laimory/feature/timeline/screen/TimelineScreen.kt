package com.soma369.laimory.feature.timeline.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.feature.timeline.BuildConfig
import com.soma369.laimory.feature.timeline.state.SourceSummaryRow
import com.soma369.laimory.feature.timeline.state.TimelineUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineUiState
import com.soma369.laimory.feature.timeline.state.UploadTarget
import com.soma369.laimory.feature.timeline.viewmodel.TimelineViewModel
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TimelineRoute(
    innerPadding: PaddingValues,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TimelineContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun TimelineContent(
    innerPadding: PaddingValues,
    state: TimelineUiState,
    onIntent: (TimelineUiIntent) -> Unit,
    sideEffectFlow: Flow<TimelineUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is TimelineUiSideEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    TimelineScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
    )

    if (state.showDatePicker) {
        TimelineDatePickerDialog(
            initialDate = state.selectedDate,
            onDismiss = { onIntent(TimelineUiIntent.DismissDatePicker) },
            onDateSelected = { onIntent(TimelineUiIntent.SelectDate(it)) },
        )
    }

    state.pendingUpload?.let { target ->
        UploadConfirmDialog(
            target = target,
            onConfirm = { onIntent(TimelineUiIntent.ConfirmUpload) },
            onDismiss = { onIntent(TimelineUiIntent.DismissUpload) },
        )
    }
}

@Composable
private fun TimelineScreen(
    innerPadding: PaddingValues,
    state: TimelineUiState,
    onIntent: (TimelineUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DateSelectorRow(
            selectedDate = state.selectedDate,
            onPickDate = { onIntent(TimelineUiIntent.ShowDatePicker) },
        )

        SummaryPanel(rows = state.summaryRows)

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { onIntent(TimelineUiIntent.RequestUpload(UploadTarget.SERVER_DRAFT)) },
                modifier = Modifier.weight(1f),
            ) {
                Text(UploadTarget.SERVER_DRAFT.label)
            }
            // 임시 테스트 전용(삭제 예정) — Drive 내보내기 버튼은 debug 빌드에만 노출한다.
            if (BuildConfig.DEBUG) {
                OutlinedButton(
                    onClick = { onIntent(TimelineUiIntent.RequestUpload(UploadTarget.DRIVE_TEST)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(UploadTarget.DRIVE_TEST.label)
                }
            }
        }
    }
}

@Composable
private fun DateSelectorRow(
    selectedDate: LocalDate,
    onPickDate: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = "모은 데이터", style = MaterialTheme.typography.titleLarge)
            Text(text = selectedDate.toString(), style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onPickDate) {
            Text("날짜 선택")
        }
    }
}

@Composable
private fun SummaryPanel(rows: List<SourceSummaryRow>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (rows.isEmpty()) {
                Text(text = "표시할 요약이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            } else {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = row.label, style = MaterialTheme.typography.bodyLarge)
                        Text(text = "${row.count}건", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun UploadConfirmDialog(
    target: UploadTarget,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${target.label} 실행") },
        text = { Text("모은 데이터를 외부로 전송합니다. 계속할까요?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("실행")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}
