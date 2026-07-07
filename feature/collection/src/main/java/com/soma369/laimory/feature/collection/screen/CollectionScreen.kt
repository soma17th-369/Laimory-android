package com.soma369.laimory.feature.collection.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.feature.collection.state.CollectionUiIntent
import com.soma369.laimory.feature.collection.state.CollectionUiSideEffect
import com.soma369.laimory.feature.collection.state.CollectionUiState
import com.soma369.laimory.feature.collection.viewmodel.CollectionViewModel
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CollectionRoute(
    innerPadding: PaddingValues,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectionContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun CollectionContent(
    innerPadding: PaddingValues,
    state: CollectionUiState,
    onIntent: (CollectionUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<CollectionUiSideEffect>,
) {
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        snackbarFlow.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    LaunchedEffect(Unit) {
        sideEffectFlow.collect { effect ->
            when (effect) {
                is CollectionUiSideEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    CollectionScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
    )
}

@Composable
private fun CollectionScreen(
    innerPadding: PaddingValues,
    state: CollectionUiState,
    onIntent: (CollectionUiIntent) -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else {
        CollectionScreenBody(
            innerPadding = innerPadding,
            state = state,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun CollectionScreenBody(
    innerPadding: PaddingValues,
    state: CollectionUiState,
    onIntent: (CollectionUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
    ) {
        Text(
            text = "수집 데이터 (${state.items.size}건)",
            style = MaterialTheme.typography.titleLarge,
        )

        Button(
            modifier = Modifier.padding(top = 8.dp),
            onClick = { onIntent(CollectionUiIntent.InsertTestItems) },
        ) {
            Text("테스트 아이템 삽입")
        }

        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "저장된 수집 아이템이 없습니다.\n수집기 연결 전에는 테스트 아이템으로 저장 경로를 확인할 수 있습니다.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { it.rawId }) { item ->
                    SourceItemRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun SourceItemRow(item: SourceItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${item.itemType} · ${item.sourceName}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "startAt: ${item.startAt.render(item.timeZoneId)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "sourceKey: ${item.sourceKey}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "payload: ${item.payload}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private val rowTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/** 저장 원본(UTC instant)을 아이템의 timezone으로 렌더링한다. */
private fun Instant.render(zoneId: ZoneId): String = atZone(zoneId).toLocalDateTime().format(rowTimeFormatter)
