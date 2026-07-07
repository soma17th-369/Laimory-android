package com.soma369.laimory.feature.collection.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.feature.collection.state.CollectionUiIntent
import com.soma369.laimory.feature.collection.state.CollectionUiSideEffect
import com.soma369.laimory.feature.collection.state.CollectionUiState
import com.soma369.laimory.feature.collection.viewmodel.CollectionViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 수집 실험실의 "사진" 탭. [CollectionLabRoute] 가 호스트하며, 화면 인셋(innerPadding)은 컨테이너가 소유하므로
 * 이 탭은 전달받은 슬롯을 채우고 자체 여백만 준다.
 */
@Composable
fun PhotoCollectionTab(viewModel: CollectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PhotoCollectionContent(
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun PhotoCollectionContent(
    state: CollectionUiState,
    onIntent: (CollectionUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<CollectionUiSideEffect>,
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
                is CollectionUiSideEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // 사진 조회/수집은 미디어 읽기 권한이 필요하다. 날짜 피커를 열기 전에 권한을 먼저 확보한다(Collector 는 권한 무지성).
    var showDatePicker by remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (PhotoPermission.canCollect(result)) {
                showDatePicker = true
            } else {
                scope.launch { snackbarHostState.showSnackbar("사진 접근 권한이 필요합니다.") }
            }
        }
    val onPickDate: () -> Unit = {
        if (PhotoPermission.canCollect(context)) {
            showDatePicker = true
        } else {
            permissionLauncher.launch(PhotoPermission.required())
        }
    }

    PhotoCollectionScreen(
        state = state,
        onIntent = onIntent,
        onPickDate = onPickDate,
    )

    if (showDatePicker) {
        PhotoDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                showDatePicker = false
                onIntent(CollectionUiIntent.SelectDate(date))
            },
        )
    }

    if (state.isSheetVisible) {
        PhotoSelectSheet(
            state = state,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun PhotoCollectionScreen(
    state: CollectionUiState,
    onIntent: (CollectionUiIntent) -> Unit,
    onPickDate: () -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
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
        DateSelectionPanel(
            selectedDate = state.selectedDate,
            candidateCount = state.candidates.size,
            isBusy = state.isBusy,
            onPickDate = onPickDate,
            onCollectAll = { onIntent(CollectionUiIntent.CollectAllOnDate) },
            onOpenSelect = { onIntent(CollectionUiIntent.OpenSelectSheet) },
        )

        StagingSection(
            stagedPhotos = state.stagedPhotos,
            isBusy = state.isBusy,
            onClear = { onIntent(CollectionUiIntent.ClearStaged) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DateSelectionPanel(
    selectedDate: LocalDate?,
    candidateCount: Int,
    isBusy: Boolean,
    onPickDate: () -> Unit,
    onCollectAll: () -> Unit,
    onOpenSelect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onPickDate, enabled = !isBusy) { Text("날짜 선택") }
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        }

        if (selectedDate != null) {
            Text(
                text = "선택 날짜: $selectedDate · 후보 ${candidateCount}장",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCollectAll, enabled = candidateCount > 0 && !isBusy) {
                    Text("그날 전부 수집")
                }
                OutlinedButton(onClick = onOpenSelect, enabled = candidateCount > 0 && !isBusy) {
                    Text("선택하여 수집")
                }
            }
        }
    }
}

@Composable
private fun StagingSection(
    stagedPhotos: List<SourceItem>,
    isBusy: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "스테이징 사진 (${stagedPhotos.size}건)",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(onClick = onClear, enabled = stagedPhotos.isNotEmpty() && !isBusy) {
                Text("일괄 삭제")
            }
        }

        if (stagedPhotos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "스테이징된 사진이 없습니다.\n날짜를 선택해 사진을 수집하세요.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(stagedPhotos, key = { it.rawId }) { item ->
                    PhotoThumbnail(uri = item.photoUri())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSelectSheet(
    state: CollectionUiState,
    onIntent: (CollectionUiIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onIntent(CollectionUiIntent.DismissSelectSheet) },
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "수집할 사진 선택 (${state.selectedIds.size}/${state.candidates.size})",
                style = MaterialTheme.typography.titleMedium,
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.candidates, key = { it.id }) { candidate ->
                    SelectableThumbnail(
                        candidate = candidate,
                        selected = candidate.id in state.selectedIds,
                        onToggle = { onIntent(CollectionUiIntent.ToggleCandidate(candidate.id)) },
                    )
                }
            }
            Button(
                onClick = { onIntent(CollectionUiIntent.CollectSelected) },
                enabled = state.selectedIds.isNotEmpty() && !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("선택 수집 (${state.selectedIds.size})")
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(uri: String?) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun SelectableThumbnail(
    candidate: PhotoCandidate,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle),
    ) {
        AsyncImage(
            model = candidate.contentUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    } ?: onDismiss()
                },
            ) { Text("선택") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

/** 스테이징 아이템의 표시용 사진 URI. PHOTO 아이템이므로 [PhotoPayload] 를 기대하되 방어적으로 캐스팅한다. */
private fun SourceItem.photoUri(): String? = (payload as? PhotoPayload)?.clientPhotoUri
