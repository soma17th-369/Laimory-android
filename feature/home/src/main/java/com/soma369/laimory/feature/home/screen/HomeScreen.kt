package com.soma369.laimory.feature.home.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomePhotoItem
import com.soma369.laimory.feature.home.state.HomeTimeField
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiSideEffect
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeRoute(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.extraLarge, vertical = Spacing.extraLarge2),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge2),
    ) {
        Text(
            text = "안녕하세요",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DateHeaderCard(
            state = state,
            onClick = { onIntent(HomeUiIntent.OpenDraftSheet) },
            onPhotoClick = { onIntent(HomeUiIntent.OpenPhotoSheet) },
        )

        TextButton(
            onClick = { onIntent(HomeUiIntent.NavigateToCollection) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("수집 데이터 자세히 보기")
        }
    }
}

/** Figma DateHeaderCard(760:2008)를 홈의 실제 요약 데이터와 생성 상태에 연결한 카드. */
@Composable
private fun DateHeaderCard(
    state: HomeUiState,
    onClick: () -> Unit,
    onPhotoClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = state.draftStatus != DraftCreationStatus.SUBMITTING,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                    Text(
                        text = cardDateLabel(state.selectedDate),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TimeRangeChip(state)
            }

            Text(
                text = momentTitle(state.selectedDate),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            )

            PhotoPreviewRow(
                previewUris = state.summary.photoPreviewUris,
                photoCount = state.summary.photoCount,
                onClick = onPhotoClick,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        "사진 ${state.summary.photoCount} · 일정 ${state.summary.calendarCount} · " +
                            "걸음 ${formatNumber(state.summary.stepCount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = draftActionLabel(state.draftStatus),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ico_default_caret_right),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRangeChip(state: HomeUiState) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = Spacing.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ico_timeline_clock),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = timeRangeLabel(state),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhotoPreviewRow(
    previewUris: List<String>,
    photoCount: Int,
    onClick: () -> Unit,
) {
    if (previewUris.isEmpty()) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ico_timeline_photo),
                    contentDescription = "초안에 사용할 사진 선택",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        previewUris.forEachIndexed { index, uri ->
            PhotoPreview(
                uri = uri,
                contentDescription = "모은 사진 미리보기 ${index + 1}",
            )
        }
        val remaining = photoCount - previewUris.size
        if (remaining > 0) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+$remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSelectionSheet(
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = remember { ZoneId.systemDefault() }
    val photosByDate =
        remember(state.availablePhotos, zone) {
            state.availablePhotos
                .groupBy { it.capturedAt.atZone(zone).toLocalDate() }
                .toSortedMap()
        }
    val isAllSelected =
        state.availablePhotos.isNotEmpty() &&
            state.pendingPhotoIds.size == state.availablePhotos.size
    ModalBottomSheet(
        onDismissRequest = { onIntent(HomeUiIntent.DismissPhotoSheet) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.extraLarge, vertical = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text(
                    text = "사진 선택",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${timeRangeLabel(state)} 사이에 모은 사진만 표시해요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.availablePhotos.isEmpty()) {
                EmptyPhotoSelection()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${state.pendingPhotoIds.size}/${state.availablePhotos.size}장 선택",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TextButton(onClick = { onIntent(HomeUiIntent.ToggleAllPhotos) }) {
                        Text(if (isAllSelected) "전체 해제" else "전체 선택")
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 88.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 88.dp, max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                ) {
                    photosByDate.forEach { (date, photos) ->
                        item(
                            key = "date-$date",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            PhotoDateHeader(
                                date = date,
                                selectedDate = state.selectedDate,
                                selectedCount = photos.count { it.rawId in state.pendingPhotoIds },
                                photoCount = photos.size,
                                onToggleAll = { onIntent(HomeUiIntent.TogglePhotoDate(date)) },
                            )
                        }
                        items(photos, key = HomePhotoItem::rawId) { photo ->
                            SelectablePhoto(
                                photo = photo,
                                selected = photo.rawId in state.pendingPhotoIds,
                                onClick = { onIntent(HomeUiIntent.TogglePhoto(photo.rawId)) },
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onIntent(HomeUiIntent.ConfirmPhotoSelection) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.availablePhotos.isEmpty()) {
                        "확인"
                    } else {
                        "${state.pendingPhotoIds.size}장 선택 완료"
                    },
                )
            }
            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

@Composable
private fun PhotoDateHeader(
    date: LocalDate,
    selectedDate: LocalDate,
    selectedCount: Int,
    photoCount: Int,
    onToggleAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = photoDateLabel(date, selectedDate),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$selectedCount/${photoCount}장 선택",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onToggleAll) {
            Text(if (selectedCount == photoCount) "이 날짜 해제" else "이 날짜 전체 선택")
        }
    }
}

@Composable
private fun EmptyPhotoSelection() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.extraLarge2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ico_timeline_photo),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "선택한 범위에 사진이 없어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectablePhoto(
    photo: HomePhotoItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = if (selected) "선택된 사진" else "선택되지 않은 사진",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.extraSmall)
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun PhotoPreview(
    uri: String,
    contentDescription: String,
) {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftSettingsSheet(
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isValid = state.recordDateWindow(ZoneId.systemDefault()) != null
    ModalBottomSheet(
        onDismissRequest = { onIntent(HomeUiIntent.DismissDraftSheet) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.extraLarge, vertical = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text("초안 범위 설정", style = MaterialTheme.typography.titleLarge)
                Text(
                    "선택한 날짜의 시작 시각부터 당일 또는 익일 종료 시각까지 모은 데이터를 사용해요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingRow(
                label = "기준 날짜",
                value = state.selectedDate.format(DATE_FORMAT),
                onClick = { onIntent(HomeUiIntent.ShowDatePicker) },
            )
            SettingRow(
                label = "시작 시각",
                value = state.startTime.format(TIME_FORMAT),
                onClick = { onIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.START)) },
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text(
                    "종료일",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    FilterChip(
                        selected = state.endDay == DraftEndDay.SAME_DAY,
                        onClick = { onIntent(HomeUiIntent.SelectEndDay(DraftEndDay.SAME_DAY)) },
                        label = { Text("당일") },
                    )
                    FilterChip(
                        selected = state.endDay == DraftEndDay.NEXT_DAY,
                        onClick = { onIntent(HomeUiIntent.SelectEndDay(DraftEndDay.NEXT_DAY)) },
                        label = { Text("익일") },
                    )
                }
            }
            SettingRow(
                label = "종료 시각",
                value = state.endTime.format(TIME_FORMAT),
                onClick = { onIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.END)) },
            )

            if (!isValid) {
                Text(
                    "종료 시각은 시작 시각보다 뒤로 설정해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(
                text = "선택 범위에 모인 데이터 ${state.summary.totalItemCount}건",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { onIntent(HomeUiIntent.CreateDraft) },
                enabled =
                    isValid &&
                        state.summary.totalItemCount > 0 &&
                        state.draftStatus != DraftCreationStatus.SUBMITTING &&
                        state.draftStatus != DraftCreationStatus.SUBMITTED,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.draftStatus == DraftCreationStatus.SUBMITTING) {
                        "초안 생성 중…"
                    } else if (state.draftStatus == DraftCreationStatus.SUBMITTED) {
                        "초안 생성 요청 완료"
                    } else {
                        "이 범위로 초안 만들기"
                    },
                )
            }
            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDatePickerDialog(
    initialDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val tomorrowMillis =
        remember {
            LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis < tomorrowMillis
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onSelect(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) {
                Text("확인")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text("확인")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        text = { TimePicker(state = pickerState) },
    )
}

private fun cardDateLabel(date: LocalDate): String = date.format(CARD_DATE_FORMAT)

private fun momentTitle(date: LocalDate): String {
    val today = LocalDate.now()
    val subject =
        when (date) {
            today -> "오늘의 순간들을"
            today.minusDays(1) -> "어제의 순간들을"
            else -> "${date.monthValue}월 ${date.dayOfMonth}일의 순간들을"
        }
    return "$subject\n모아봤어요."
}

private fun timeRangeLabel(state: HomeUiState): String {
    val endPrefix = if (state.endDay == DraftEndDay.NEXT_DAY) "익일 " else ""
    return "${state.startTime.format(TIME_FORMAT)} ~ $endPrefix${state.endTime.format(TIME_FORMAT)}"
}

private fun photoDateLabel(
    date: LocalDate,
    selectedDate: LocalDate,
): String {
    val relation =
        when (date) {
            selectedDate -> if (date == LocalDate.now()) "오늘" else "기준일"
            selectedDate.plusDays(1) -> "익일"
            else -> null
        }
    val dateLabel = "${date.monthValue}월 ${date.dayOfMonth}일"
    return relation?.let { "$dateLabel · $it" } ?: dateLabel
}

private fun draftActionLabel(status: DraftCreationStatus): String =
    when (status) {
        DraftCreationStatus.IDLE -> "초안 만들기"
        DraftCreationStatus.SUBMITTING -> "생성 중"
        DraftCreationStatus.SUBMITTED -> "요청 완료"
        DraftCreationStatus.FAILED -> "다시 시도"
    }

private fun formatNumber(value: Long): String = String.format(Locale.KOREA, "%,d", value)

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
private val CARD_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREA)
