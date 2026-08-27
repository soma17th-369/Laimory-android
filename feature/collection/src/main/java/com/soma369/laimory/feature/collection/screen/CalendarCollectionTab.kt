package com.soma369.laimory.feature.collection.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.util.permission.CalendarPermission
import com.soma369.laimory.feature.collection.state.CalendarUiIntent
import com.soma369.laimory.feature.collection.state.CalendarUiSideEffect
import com.soma369.laimory.feature.collection.state.CalendarUiState
import com.soma369.laimory.feature.collection.viewmodel.CalendarCollectionViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * 수집 실험실의 "일정" 탭. 개인(owner-access) 캘린더의 최근 한 달 일정을 수집해 스테이징 목록으로 확인한다.
 * 화면 인셋은 컨테이너([CollectionLabRoute])가 소유하므로 이 탭은 슬롯을 채우고 자체 여백만 준다.
 */
@Composable
fun CalendarCollectionTab(viewModel: CalendarCollectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarCollectionContent(
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun CalendarCollectionContent(
    state: CalendarUiState,
    onIntent: (CalendarUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<CalendarUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snackbarFlow.collect { message -> snackbarHostState.showSnackbar(message) }
    }
    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is CalendarUiSideEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // 일정 조회는 READ_CALENDAR 가 필요하다. 수집 트리거 전에 권한을 먼저 확보한다(Collector 는 권한 무지성).
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (CalendarPermission.isGranted(result)) {
                onIntent(CalendarUiIntent.Collect)
            } else {
                scope.launch { snackbarHostState.showSnackbar("캘린더 접근 권한이 필요합니다.") }
            }
        }
    val onCollect: () -> Unit = {
        if (CalendarPermission.isGranted(context)) {
            onIntent(CalendarUiIntent.Collect)
        } else {
            permissionLauncher.launch(CalendarPermission.required())
        }
    }

    CalendarCollectionScreen(
        state = state,
        onCollect = onCollect,
        onClear = { onIntent(CalendarUiIntent.ClearStaged) },
    )
}

@Composable
private fun CalendarCollectionScreen(
    state: CalendarUiState,
    onCollect: () -> Unit,
    onClear: () -> Unit,
) {
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onCollect, enabled = !state.isBusy) { Text("개인 일정 수집 (최근 한 달)") }
            if (state.isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "스테이징 일정 (${state.stagedEvents.size}건)",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(onClick = onClear, enabled = state.stagedEvents.isNotEmpty() && !state.isBusy) {
                Text("일괄 삭제")
            }
        }

        if (state.stagedEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "스테이징된 일정이 없습니다.\n개인 캘린더 일정을 수집하세요.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.stagedEvents, key = { it.rawId }) { item -> CalendarEventItem(item) }
            }
        }
    }
}

@Composable
private fun CalendarEventItem(item: SourceItem) {
    val payload = item.payload as? CalendarPayload
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = payload?.title?.ifBlank { "(제목 없음)" } ?: "(제목 없음)",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = item.timeRangeText(payload?.allDay == true),
                style = MaterialTheme.typography.bodySmall,
            )
            payload?.locationText?.takeIf { it.isNotBlank() }?.let { location ->
                Text(text = location, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private val dateTimeFormatter = DateTimeFormatter.ofPattern("M/d(E) HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("M/d(E)")

/** 일정 시작~종료를 아이템 타임존 기준 문자열로 표시한다. 종일 일정은 시각 없이 날짜만 보여준다. */
private fun SourceItem.timeRangeText(allDay: Boolean): String {
    val formatter = if (allDay) dateFormatter else dateTimeFormatter
    val start = startAt.atZone(timeZoneId).format(formatter)
    val end = endAt?.atZone(timeZoneId)?.format(formatter)
    return if (end != null) "$start ~ $end" else start
}
