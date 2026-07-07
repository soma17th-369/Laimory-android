package com.soma369.laimory.feature.collection.screen

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.feature.collection.state.HealthUiIntent
import com.soma369.laimory.feature.collection.state.HealthUiSideEffect
import com.soma369.laimory.feature.collection.state.HealthUiState
import com.soma369.laimory.feature.collection.viewmodel.HealthCollectionViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * 수집 실험실의 "헬스" 탭. Health Connect 로 최근 한 달 걸음수(일별)·수면(세션)을 수집해 스테이징 목록으로 확인한다.
 * 화면 인셋은 컨테이너([CollectionLabRoute])가 소유하므로 이 탭은 슬롯을 채우고 자체 여백만 준다.
 */
@Composable
fun HealthCollectionTab(viewModel: HealthCollectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HealthCollectionContent(
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun HealthCollectionContent(
    state: HealthUiState,
    onIntent: (HealthUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<HealthUiSideEffect>,
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
                is HealthUiSideEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // Health Connect 는 자체 권한 모델을 쓴다. 수집 트리거 전에 전용 contract 로 권한을 확보한다(Collector 는 권한 무지성).
    val permissionLauncher =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.containsAll(HealthPermissions.required)) {
                onIntent(HealthUiIntent.Collect)
            } else {
                scope.launch { snackbarHostState.showSnackbar("건강 데이터 권한이 필요합니다.") }
            }
        }
    val onCollect: () -> Unit = {
        if (HealthPermissions.isAvailable(context)) {
            // 이미 허용돼 있으면 즉시 결과가 돌아오고, 아니면 Health Connect 권한 화면을 연다.
            permissionLauncher.launch(HealthPermissions.required)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Health Connect를 사용할 수 없습니다(미설치/업데이트 필요).") }
        }
    }

    HealthCollectionScreen(
        state = state,
        onCollect = onCollect,
        onClear = { onIntent(HealthUiIntent.ClearStaged) },
    )
}

@Composable
private fun HealthCollectionScreen(
    state: HealthUiState,
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
            Button(onClick = onCollect, enabled = !state.isBusy) { Text("건강 데이터 수집 (최근 한 달)") }
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
                text = "스테이징 건강 데이터 (${state.stagedHealth.size}건)",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(onClick = onClear, enabled = state.stagedHealth.isNotEmpty() && !state.isBusy) {
                Text("일괄 삭제")
            }
        }

        if (state.stagedHealth.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "스테이징된 건강 데이터가 없습니다.\n걸음수·수면을 수집하세요.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.stagedHealth, key = { it.rawId }) { item -> HealthItem(item) }
            }
        }
    }
}

@Composable
private fun HealthItem(item: SourceItem) {
    val payload = item.payload as? HealthPayload ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = payload.metric.label(), style = MaterialTheme.typography.titleSmall)
                Text(text = payload.displayValue(), style = MaterialTheme.typography.titleSmall)
            }
            Text(text = item.healthTimeText(payload.metric), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun HealthPayload.Metric.label(): String =
    when (this) {
        HealthPayload.Metric.STEPS -> "걸음수"
        HealthPayload.Metric.SLEEP -> "수면"
    }

/** 저장은 raw value + canonical unit(count/minute). 표시 문자열은 여기(UI)에서 metric 보고 렌더한다. */
private fun HealthPayload.displayValue(): String =
    when (metric) {
        HealthPayload.Metric.STEPS -> "${value.toInt()}보"
        HealthPayload.Metric.SLEEP -> "${value.toInt()}분"
    }

private val dateFormatter = DateTimeFormatter.ofPattern("M/d(E)")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 걸음수는 일자만, 수면은 날짜+시작~종료 시각으로 표시한다(아이템 타임존 기준). */
private fun SourceItem.healthTimeText(metric: HealthPayload.Metric): String {
    val start = startAt.atZone(timeZoneId)
    return when (metric) {
        HealthPayload.Metric.STEPS -> start.format(dateFormatter)
        HealthPayload.Metric.SLEEP -> {
            val end = endAt?.atZone(timeZoneId)?.format(timeFormatter)
            "${start.format(dateFormatter)} ${start.format(timeFormatter)}${if (end != null) " ~ $end" else ""}"
        }
    }
}
