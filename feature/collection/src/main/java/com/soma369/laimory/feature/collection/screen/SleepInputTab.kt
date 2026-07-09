package com.soma369.laimory.feature.collection.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.feature.collection.state.SleepInputUiIntent
import com.soma369.laimory.feature.collection.state.SleepInputUiSideEffect
import com.soma369.laimory.feature.collection.state.SleepInputUiState
import com.soma369.laimory.feature.collection.state.SleepTimeField
import com.soma369.laimory.feature.collection.viewmodel.SleepInputViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 수집 실험실의 "수면" 탭(#145). 불확실한 밤의 취침·기상을 사용자가 입력해 Health Connect 에 기록한다.
 * 화면 인셋은 컨테이너([CollectionLabRoute])가 소유하므로 이 탭은 슬롯을 채우고 자체 여백만 준다.
 */
@Composable
fun SleepInputTab(viewModel: SleepInputViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SleepInputContent(
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
        sideEffectFlow = viewModel.sideEffect,
    )
}

@Composable
private fun SleepInputContent(
    state: SleepInputUiState,
    onIntent: (SleepInputUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
    sideEffectFlow: Flow<SleepInputUiSideEffect>,
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
                is SleepInputUiSideEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // Health Connect 쓰기 권한(sleepProducer)을 저장 전에 확보한다(Collector 계약처럼 화면이 권한을 책임).
    val permissionLauncher =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.containsAll(HealthPermissions.sleepProducer)) {
                onIntent(SleepInputUiIntent.Save)
            } else {
                scope.launch { snackbarHostState.showSnackbar("수면 기록 권한이 필요합니다.") }
            }
        }
    val onSaveClick: () -> Unit = {
        if (HealthPermissions.isAvailable(context)) {
            permissionLauncher.launch(HealthPermissions.sleepProducer)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Health Connect를 사용할 수 없습니다(미설치/업데이트 필요).") }
        }
    }

    SleepInputScreen(state = state, onIntent = onIntent, onSave = onSaveClick)

    state.editingField?.let { field ->
        val initial = if (field == SleepTimeField.BED) state.bedTime else state.wakeTime
        SleepTimePickerDialog(
            initial = initial,
            onConfirm = { time ->
                onIntent(
                    if (field == SleepTimeField.BED) {
                        SleepInputUiIntent.SetBedTime(time)
                    } else {
                        SleepInputUiIntent.SetWakeTime(time)
                    },
                )
            },
            onDismiss = { onIntent(SleepInputUiIntent.DismissTimePicker) },
        )
    }
}

@Composable
private fun SleepInputScreen(
    state: SleepInputUiState,
    onIntent: (SleepInputUiIntent) -> Unit,
    onSave: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val durationMinutes = SleepInputMath.durationMinutes(state.wakeDate, state.bedTime, state.wakeTime, zone)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DateHeader(wakeDate = state.wakeDate)

        SleepRing(
            bedTime = state.bedTime,
            wakeTime = state.wakeTime,
            modifier =
                Modifier
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1f),
        )

        DurationChip(minutes = durationMinutes)

        if (state.alreadyRecorded) {
            Text(
                text = "이미 이 밤 수면 기록이 있어요. 저장하면 덮어씁니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        SleepTimeRow(
            emoji = "🌙",
            label = "취침 시간",
            time = state.bedTime,
            onClick = { onIntent(SleepInputUiIntent.ShowTimePicker(SleepTimeField.BED)) },
        )
        SleepTimeRow(
            emoji = "⏰",
            label = "기상 시간",
            time = state.wakeTime,
            onClick = { onIntent(SleepInputUiIntent.ShowTimePicker(SleepTimeField.WAKE)) },
        )

        Spacer(modifier = Modifier.size(8.dp))

        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("저장")
            }
        }
    }
}

@Composable
private fun DateHeader(wakeDate: LocalDate) {
    val today = remember { LocalDate.now() }
    val label =
        when (wakeDate) {
            today -> "오늘"
            today.minusDays(1) -> "어제"
            else -> wakeDate.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))
        }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = wakeDate.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SleepRing(
    bedTime: LocalTime,
    wakeTime: LocalTime,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = MaterialTheme.colorScheme.primary
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 20.dp.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            drawArc(
                color = arcColor,
                startAngle = angleOf(bedTime),
                sweepAngle = sweepBetween(bedTime, wakeTime),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "🌙 ${bedTime.format(TIME_FORMAT)}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "⏰ ${wakeTime.format(TIME_FORMAT)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DurationChip(minutes: Long) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = "수면 시간 ${durationLabel(minutes)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SleepTimeRow(
    emoji: String,
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = emoji)
            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = time.format(TIME_FORMAT),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = "›", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = pickerState)
            }
        },
    )
}

/** 24시간 다이얼(0시=위, 시계방향)에서 [time] 의 각도(Compose degrees, 0°=3시 방향). */
private fun angleOf(time: LocalTime): Float {
    val hours = time.hour + time.minute / 60f
    return hours / 24f * 360f - 90f
}

/** 취침→기상까지 시계방향 sweep 각도. */
private fun sweepBetween(
    bed: LocalTime,
    wake: LocalTime,
): Float {
    val bedHours = bed.hour + bed.minute / 60f
    val wakeHours = wake.hour + wake.minute / 60f
    val diff = ((wakeHours - bedHours) % 24f + 24f) % 24f
    val span = if (diff == 0f) 24f else diff
    return span / 24f * 360f
}

private fun durationLabel(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (mins == 0L) "${hours}시간" else "${hours}시간 ${mins}분"
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
