package com.soma369.laimory.feature.collection.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 수집 실험실의 "수면" 탭(#145). 불확실한 밤의 취침·기상을 사용자가 입력해 Health Connect 에 기록한다.
 * 원형 다이얼의 두 핸들을 드래그하거나 시간 행을 눌러 정밀 입력할 수 있고, 날짜는 이전/다음·달력으로 고른다.
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

    if (state.showDatePicker) {
        SleepDatePickerDialog(
            initial = state.wakeDate,
            onSelect = { onIntent(SleepInputUiIntent.SelectDate(it)) },
            onDismiss = { onIntent(SleepInputUiIntent.DismissDatePicker) },
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
                .padding(20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DateHeader(
                wakeDate = state.wakeDate,
                canGoNext = state.wakeDate.isBefore(LocalDate.now()),
                onPrev = { onIntent(SleepInputUiIntent.PreviousDay) },
                onNext = { onIntent(SleepInputUiIntent.NextDay) },
                onPickDate = { onIntent(SleepInputUiIntent.ShowDatePicker) },
            )

            SleepDialRing(
                bedTime = state.bedTime,
                wakeTime = state.wakeTime,
                onBedTimeChange = { onIntent(SleepInputUiIntent.SetBedTime(it)) },
                onWakeTimeChange = { onIntent(SleepInputUiIntent.SetWakeTime(it)) },
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
        }

        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
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
private fun DateHeader(
    wakeDate: LocalDate,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickDate: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val label =
        when (wakeDate) {
            today -> "오늘"
            today.minusDays(1) -> "어제"
            else -> wakeDate.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN))
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onPrev) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
        Column(
            modifier = Modifier.clickable(onClick = onPickDate),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = wakeDate.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onNext, enabled = canGoNext) {
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun SleepDialRing(
    bedTime: LocalTime,
    wakeTime: LocalTime,
    onBedTimeChange: (LocalTime) -> Unit,
    onWakeTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = MaterialTheme.colorScheme.primary
    val handleFill = MaterialTheme.colorScheme.onPrimary
    val bed by rememberUpdatedState(bedTime)
    val wake by rememberUpdatedState(wakeTime)
    var active by remember { mutableStateOf<SleepTimeField?>(null) }

    Box(
        modifier =
            modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = (size.width.coerceAtMost(size.height) - RING_STROKE.toPx()) / 2f
                        val bedHandle = polar(center, radius, angleOf(bed))
                        val wakeHandle = polar(center, radius, angleOf(wake))
                        active =
                            if ((position - bedHandle).getDistance() <= (position - wakeHandle).getDistance()) {
                                SleepTimeField.BED
                            } else {
                                SleepTimeField.WAKE
                            }
                    },
                    onDragEnd = { active = null },
                    onDragCancel = { active = null },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val time = timeAtOffset(change.position, center)
                        when (active) {
                            SleepTimeField.BED -> onBedTimeChange(time)
                            SleepTimeField.WAKE -> onWakeTimeChange(time)
                            null -> Unit
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = RING_STROKE.toPx()
            val diameter = size.minDimension - strokePx
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(diameter, diameter)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokePx))
            drawArc(
                color = arcColor,
                startAngle = angleOf(bed),
                sweepAngle = sweepBetween(bed, wake),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            val handleRadius = strokePx * 0.62f
            listOf(bed, wake).forEach { time ->
                val at = polar(center, radius, angleOf(time))
                drawCircle(handleFill, handleRadius, at)
                drawCircle(arcColor, handleRadius, at, style = Stroke(strokePx * 0.18f))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "🌙 ${bed.format(TIME_FORMAT)}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "⏰ ${wake.format(TIME_FORMAT)}", style = MaterialTheme.typography.bodyMedium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepDatePickerDialog(
    initial: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // 미래(오늘 이후)는 고를 수 없다 — 아직 자지 않은 밤이므로.
    val tomorrowMillis = remember { LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    ) {
        DatePicker(state = pickerState)
    }
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

/** 원의 [angleDeg](Compose degrees) 위치. */
private fun polar(
    center: Offset,
    radius: Float,
    angleDeg: Float,
): Offset {
    val radians = Math.toRadians(angleDeg.toDouble())
    return Offset(center.x + radius * cos(radians).toFloat(), center.y + radius * sin(radians).toFloat())
}

/** 드래그 스냅 단위(분). 타임피커 정밀 입력(1분)과 달리 다이얼은 이 단위로 반올림한다. */
private const val DRAG_SNAP_MINUTES = 10

/** 터치 위치 → 24시간 다이얼 시각([DRAG_SNAP_MINUTES] 반올림). */
private fun timeAtOffset(
    position: Offset,
    center: Offset,
): LocalTime {
    val angleDeg = Math.toDegrees(atan2((position.y - center.y).toDouble(), (position.x - center.x).toDouble()))
    val hours = (((angleDeg + 90.0) / 360.0 * 24.0) % 24.0 + 24.0) % 24.0
    val totalMinutes = ((hours * 60.0 / DRAG_SNAP_MINUTES).roundToInt() * DRAG_SNAP_MINUTES) % (24 * 60)
    return LocalTime.of(totalMinutes / 60, totalMinutes % 60)
}

private fun durationLabel(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (mins == 0L) "${hours}시간" else "${hours}시간 ${mins}분"
}

private val RING_STROKE: Dp = 20.dp
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
