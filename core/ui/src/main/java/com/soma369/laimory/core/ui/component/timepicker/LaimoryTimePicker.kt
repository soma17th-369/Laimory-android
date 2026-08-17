package com.soma369.laimory.core.ui.component.timepicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 빠른 조정 버튼 한 개. [minutes]는 현재 값에 그대로 더한다(스냅 없음). */
data class TimePickerQuickAdjustment(
    val label: String,
    val minutes: Long,
) {
    companion object {
        /** Figma 기본 구성: `+1시간`·`+30분`·`+5분`. */
        val Defaults: List<TimePickerQuickAdjustment> =
            listOf(
                TimePickerQuickAdjustment("+1시간", 60L),
                TimePickerQuickAdjustment("+30분", 30L),
                TimePickerQuickAdjustment("+5분", 5L),
            )
    }
}

/**
 * Laimory 공통 타임 피커.
 *
 * 헤더를 눌러 펼치는 인라인 카드다 — 확인·취소 버튼이 없고 롤러 변경이 [onValueChange]로 즉시
 * 전달되므로, 최종 저장과 폐기는 화면이 담당한다.
 *
 * [dates]가 비었거나 1개면 날짜 열을 숨기고 시·분 2열만 표시하며, 이때 시·분은 순환하되 날짜는
 * 이동하지 않는다. [quickAdjustments]가 비어 있으면 빠른 조정 행을 표시하지 않는다.
 */
@Composable
fun LaimoryTimePicker(
    value: LaimoryTimePickerValue,
    onValueChange: (LaimoryTimePickerValue) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    dates: List<TimePickerDateOption> = emptyList(),
    minuteStep: TimePickerMinuteStep = TimePickerMinuteStep.FIVE,
    quickAdjustments: List<TimePickerQuickAdjustment> = emptyList(),
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        shadowElevation = CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.extraLarge2),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            TimePickerHeader(
                value = value,
                dates = dates,
                isExpanded = isExpanded,
                enabled = enabled,
                onClick = { onExpandedChange(!isExpanded) },
            )
            if (isExpanded) {
                if (quickAdjustments.isNotEmpty()) {
                    QuickAdjustRow(
                        value = value,
                        dates = dates,
                        adjustments = quickAdjustments,
                        enabled = enabled,
                        onValueChange = onValueChange,
                    )
                }
                TimePickerRollers(
                    value = value,
                    dates = dates,
                    minuteStep = minuteStep,
                    onValueChange = onValueChange,
                )
            }
        }
    }
}

@Composable
private fun TimePickerHeader(
    value: LaimoryTimePickerValue,
    dates: List<TimePickerDateOption>,
    isExpanded: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val label = value.headerLabel(dates)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(HeaderBorderWidth, borderColor, RoundedCornerShape(HeaderCornerRadius))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = Spacing.large, vertical = 14.dp)
                .semantics {
                    contentDescription = "$label. ${if (isExpanded) "시각 선택 닫기" else "시각 선택 열기"}"
                },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ico_default_clock),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accent,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = accent,
            )
        }
        Icon(
            painter =
                painterResource(
                    if (isExpanded) R.drawable.ico_default_chevron_up else R.drawable.ico_default_chevron_down,
                ),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuickAdjustRow(
    value: LaimoryTimePickerValue,
    dates: List<TimePickerDateOption>,
    adjustments: List<TimePickerQuickAdjustment>,
    enabled: Boolean,
    onValueChange: (LaimoryTimePickerValue) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.CenterHorizontally),
    ) {
        adjustments.forEach { adjustment ->
            // 결과가 허용 날짜를 벗어나면 클램프하지 않고 버튼을 비활성화한다.
            val adjusted = LaimoryTimePickerMath.quickAdjust(value, adjustment.minutes, dates)
            Surface(
                onClick = { adjusted?.let(onValueChange) },
                enabled = enabled && adjusted != null,
                shape = RoundedCornerShape(QuickPillCornerRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
            ) {
                Text(
                    text = adjustment.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = Spacing.small),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun TimePickerRollers(
    value: LaimoryTimePickerValue,
    dates: List<TimePickerDateOption>,
    minuteStep: TimePickerMinuteStep,
    onValueChange: (LaimoryTimePickerValue) -> Unit,
) {
    val showDateColumn = dates.size > 1
    // 목록 길이가 스크롤 도중 바뀌면 위치가 튀므로, 펼친 시점의 분을 기준으로 한 번만 만든다.
    // 빠른 조정으로 간격 밖 분이 생기면 그 값도 함께 포함해 표시가 어긋나지 않게 한다.
    var extraMinutes by remember(minuteStep) { mutableStateOf(setOf(value.time.minute)) }
    LaunchedEffect(value.time.minute) {
        if (!minuteStep.contains(value.time.minute)) extraMinutes = extraMinutes + value.time.minute
    }
    val minuteOptions =
        remember(minuteStep, extraMinutes) {
            (minuteStep.options() + extraMinutes).distinct().sorted()
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(RollerCornerRadius))
                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(RollerCornerRadius)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small)) {
            if (showDateColumn) ColumnLabel("날짜")
            ColumnLabel("시 (Hour)")
            ColumnLabel("분 (Minute)")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Box(modifier = Modifier.fillMaxWidth().height(RollerHeight)) {
            SelectionBands()
            Row(modifier = Modifier.fillMaxWidth()) {
                if (showDateColumn) {
                    SpinnerColumn(
                        modifier = Modifier.weight(1f),
                        options = dates.map(TimePickerDateOption::label),
                        selectedIndex = dates.indexOfFirst { it.date == value.date }.coerceAtLeast(0),
                        isCyclic = false,
                        columnLabel = "날짜",
                        onDelta = { delta -> onValueChange(LaimoryTimePickerMath.shiftDate(value, delta, dates)) },
                    )
                }
                SpinnerColumn(
                    modifier = Modifier.weight(1f),
                    options = LaimoryTimePickerMath.hourOptions().map { "${it}시" },
                    selectedIndex = value.time.hour,
                    isCyclic = true,
                    columnLabel = "시",
                    onDelta = { delta -> onValueChange(LaimoryTimePickerMath.scrollHour(value, delta, dates)) },
                )
                SpinnerColumn(
                    modifier = Modifier.weight(1f),
                    options = minuteOptions.map { "%02d분".format(it) },
                    selectedIndex = minuteOptions.indexOf(value.time.minute).coerceAtLeast(0),
                    isCyclic = true,
                    columnLabel = "분",
                    onDelta = { delta ->
                        onValueChange(LaimoryTimePickerMath.scrollMinute(value, delta, minuteStep, dates))
                    },
                )
            }
        }
    }
}

@Composable
private fun RowScope.ColumnLabel(text: String) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectionBands() {
    Column(modifier = Modifier.fillMaxWidth().height(RollerHeight)) {
        Box(modifier = Modifier.height(RollerRowHeight))
        HorizontalDivider(color = MaterialTheme.colorScheme.primaryContainer)
        Box(modifier = Modifier.height(RollerRowHeight))
        HorizontalDivider(color = MaterialTheme.colorScheme.primaryContainer)
    }
}

/**
 * 한 열의 롤러. 드래그·fling 후 한 항목에 snap 하고, 멈춘 위치의 이동량을 [onDelta]로 알린다.
 *
 * [isCyclic]이면 목록을 크게 반복해 끝없이 돌 수 있게 하고, 아니면 목록 경계에서 멈춘다.
 * 값 변경은 이동량(delta)으로만 전달해 자정 carry 판정을 순수 모델이 담당하게 한다.
 */
@Composable
private fun SpinnerColumn(
    options: List<String>,
    selectedIndex: Int,
    isCyclic: Boolean,
    columnLabel: String,
    onDelta: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    val cycles = if (isCyclic) CYCLIC_REPEAT_COUNT else 1
    val baseIndex = if (isCyclic) options.size * (cycles / 2) else 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = baseIndex + selectedIndex)
    // 목록 구성이 바뀌면 이동량 기준점도 다시 잡는다.
    var lastIndex by remember(options.size) { mutableIntStateOf(baseIndex + selectedIndex) }

    // 스크롤이 아닌 경로(빠른 조정·다른 열의 자정 carry)로 값이 바뀌면 롤러도 따라 이동한다.
    // 같은 순환 주기 안에서 가장 가까운 자리로 옮겨 위치가 튀지 않게 한다.
    LaunchedEffect(selectedIndex, options.size) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        val target = (lastIndex / options.size) * options.size + selectedIndex
        if (listState.firstVisibleItemIndex != target) {
            listState.scrollToItem(target)
            lastIndex = target
        }
    }
    // 스크롤 중에도 가운데 행 강조가 따라오도록 실시간 인덱스를 따로 본다.
    val centerIndex by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex } }

    // 스크롤이 멈춘 뒤에만 이동량을 알려 중간 프레임마다 값이 튀지 않게 한다.
    LaunchedEffect(listState, options.size) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .filter { (scrolling, _) -> !scrolling }
            .map { (_, index) -> index }
            .distinctUntilChanged()
            .collect { index ->
                val delta = index - lastIndex
                lastIndex = index
                if (delta != 0) onDelta(delta)
            }
    }

    LazyColumn(
        modifier =
            modifier
                .height(RollerHeight)
                .semantics {
                    contentDescription = "$columnLabel 선택. 현재 ${options[centerIndex % options.size]}"
                },
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        contentPadding = PaddingValues(vertical = RollerRowHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(count = options.size * cycles) { index ->
            val option = options[index % options.size]
            val isSelected = index == centerIndex
            Box(
                modifier = Modifier.fillMaxWidth().height(RollerRowHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    modifier = if (isSelected) Modifier else Modifier.alpha(UNSELECTED_ROW_ALPHA),
                    style =
                        if (isSelected) {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                    color =
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun LaimoryTimePickerValue.headerLabel(dates: List<TimePickerDateOption>): String {
    val time = time.format(HeaderTimeFormatter)
    return if (dates.size > 1) "(${date.format(HeaderDateFormatter)}) $time" else time
}

private val HeaderDateFormatter = DateTimeFormatter.ofPattern("MM.dd")
private val HeaderTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val CardCornerRadius = 20.dp
private val CardElevation = 2.dp
private val HeaderCornerRadius = 12.dp
private val HeaderBorderWidth = 1.5.dp
private val RollerCornerRadius = 14.dp
private val RollerHeight = 141.dp
private val RollerRowHeight = 47.dp
private val QuickPillCornerRadius = 20.dp
private const val UNSELECTED_ROW_ALPHA = 0.3f
private const val CYCLIC_REPEAT_COUNT = 201

@Preview(name = "TimePicker / Collapsed", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryTimePickerCollapsedPreview() {
    LaimoryTheme {
        LaimoryTimePicker(
            value = previewValue(),
            onValueChange = {},
            isExpanded = false,
            onExpandedChange = {},
            dates = previewDates(),
            modifier = Modifier.padding(Spacing.large),
        )
    }
}

@Preview(name = "TimePicker / Expanded", showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun LaimoryTimePickerExpandedPreview() {
    LaimoryTheme {
        LaimoryTimePicker(
            value = previewValue(),
            onValueChange = {},
            isExpanded = true,
            onExpandedChange = {},
            dates = previewDates(),
            modifier = Modifier.padding(Spacing.large),
        )
    }
}

@Preview(name = "TimePicker / Quick Adjust", showBackground = true, widthDp = 360, heightDp = 460)
@Composable
private fun LaimoryTimePickerQuickAdjustPreview() {
    LaimoryTheme {
        LaimoryTimePicker(
            value = previewValue(),
            onValueChange = {},
            isExpanded = true,
            onExpandedChange = {},
            dates = previewDates(),
            quickAdjustments = TimePickerQuickAdjustment.Defaults,
            modifier = Modifier.padding(Spacing.large),
        )
    }
}

@Preview(name = "TimePicker / 날짜 열 없음", showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun LaimoryTimePickerWithoutDateColumnPreview() {
    LaimoryTheme {
        LaimoryTimePicker(
            value = previewValue(),
            onValueChange = {},
            isExpanded = true,
            onExpandedChange = {},
            dates = listOf(TimePickerDateOption(LocalDate.of(2026, 8, 1), "당일")),
            modifier = Modifier.padding(Spacing.large),
        )
    }
}

private fun previewValue() = LaimoryTimePickerValue(date = LocalDate.of(2026, 8, 1), time = LocalTime.of(16, 10))

private fun previewDates() =
    listOf(
        TimePickerDateOption(LocalDate.of(2026, 8, 1), "당일"),
        TimePickerDateOption(LocalDate.of(2026, 8, 2), "익일"),
    )
