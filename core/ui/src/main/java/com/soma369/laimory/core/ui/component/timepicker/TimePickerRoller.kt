package com.soma369.laimory.core.ui.component.timepicker

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.tabularFigures
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * 타임 피커 롤러. 열 이름 줄과 3행이 보이는 스크롤 영역으로 구성된다.
 *
 * [dates]가 2개 이상일 때만 날짜 열을 함께 보여주고, 그보다 적으면 시·분 2열만 둔다. 날짜 열이
 * 없으면 옮길 날짜도 없으므로 시·분은 순환하되 날짜는 이동하지 않는다(순수 모델이 판정).
 */
@Composable
internal fun TimePickerRoller(
    value: LaimoryTimePickerValue,
    dates: List<TimePickerDateOption>,
    minuteStep: TimePickerMinuteStep,
    onValueChange: (LaimoryTimePickerValue, TimePickerColumn) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedRange<LocalDateTime>? = null,
) {
    // 범위 밖 선택지는 아예 내놓지 않는다 — 고를 수 있게 두고 확인에서 막으면 왜 안 되는지 알기 어렵다.
    val dateOptions = LaimoryTimePickerMath.allowedDates(dates, minuteStep, value.time.minute, range)
    val showDateColumn = dateOptions.size > 1
    // 날짜 열이 있으면 시·분은 순환하지 않는다.
    //
    // 순환하면 목록 끝을 넘는 순간 날짜가 함께 밀려야 하는데, 날짜 목록도 끝이라 밀 수 없으면 시만
    // 한 바퀴 감겨 하루 가까이 되돌아간다(익일 08시에서 앞으로 굴렸는데 익일 05시가 되는 식).
    // 값을 거부하면 이번엔 롤러만 움직여 표시와 값이 어긋나므로, 스크롤 자체를 막아 해결한다.
    // 범위가 있으면 끝에서 멈춰야 하므로 순환하지 않는다.
    val isTimeCyclic = !showDateColumn && range == null
    // 목록 길이가 스크롤 도중 바뀌면 위치가 튀므로, 간격 밖 분이 생기면 선택지에 더하기만 한다.
    var extraMinutes by remember(minuteStep) { mutableStateOf(setOf(value.time.minute)) }
    LaunchedEffect(value.time.minute, minuteStep) {
        if (!minuteStep.contains(value.time.minute)) extraMinutes = extraMinutes + value.time.minute
    }
    val minuteOptions =
        if (range == null) {
            remember(minuteStep, extraMinutes) {
                (minuteStep.options() + extraMinutes).distinct().sorted()
            }
        } else {
            LaimoryTimePickerMath.allowedMinutes(value.date, value.time.hour, minuteStep, value.time.minute, range)
        }
    val hourOptions = LaimoryTimePickerMath.allowedHours(value.date, minuteStep, value.time.minute, range)

    // 롤러가 끝에 닿아도 남은 스크롤이 시트로 번져 시트째 내려가지 않도록 여기서 삼킨다.
    // 날짜 열은 선택지가 두어 개뿐이라 경계에 자주 닿는다.
    val scrollBoundary = remember { RollerScrollBoundary() }
    Column(modifier = modifier.fillMaxWidth().nestedScroll(scrollBoundary)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small)) {
            if (showDateColumn) ColumnLabel("날짜")
            ColumnLabel("시")
            ColumnLabel("분")
        }
        Box(modifier = Modifier.fillMaxWidth().height(RollerHeight)) {
            SelectionBands()
            Row(modifier = Modifier.fillMaxWidth()) {
                if (showDateColumn) {
                    SpinnerColumn(
                        modifier = Modifier.weight(1f),
                        options = dateOptions.map(TimePickerDateOption::label),
                        selectedIndex = dateOptions.indexOfFirst { it.date == value.date }.coerceAtLeast(0),
                        isCyclic = false,
                        columnLabel = "날짜",
                        textStyle = MaterialTheme.typography.titleLarge,
                        onDelta = { delta ->
                            // 날짜를 옮기면 그 날에 고를 수 없는 시각이 될 수 있어 범위 안으로 붙인다.
                            val shifted = LaimoryTimePickerMath.shiftDate(value, delta, dateOptions)
                            onValueChange(
                                LaimoryTimePickerMath.coerceIntoRange(shifted, range),
                                TimePickerColumn.DATE,
                            )
                        },
                    )
                }
                SpinnerColumn(
                    modifier = Modifier.weight(1f),
                    options = hourOptions.map { "%02d".format(it) },
                    selectedIndex = hourOptions.indexOf(value.time.hour).coerceAtLeast(0),
                    isCyclic = isTimeCyclic,
                    columnLabel = "시",
                    textStyle = MaterialTheme.typography.headlineMedium,
                    onDelta = { delta ->
                        val next =
                            if (range == null) {
                                LaimoryTimePickerMath.scrollHour(value, delta, dateOptions)
                            } else {
                                val hour = LaimoryTimePickerMath.scrollWithin(hourOptions, value.time.hour, delta)
                                LaimoryTimePickerMath.coerceIntoRange(
                                    value.copy(time = value.time.withHour(hour)),
                                    range,
                                )
                            }
                        onValueChange(next, TimePickerColumn.HOUR)
                    },
                )
                SpinnerColumn(
                    modifier = Modifier.weight(1f),
                    options = minuteOptions.map { "%02d".format(it) },
                    selectedIndex = minuteOptions.indexOf(value.time.minute).coerceAtLeast(0),
                    isCyclic = isTimeCyclic,
                    columnLabel = "분",
                    textStyle = MaterialTheme.typography.headlineMedium,
                    onDelta = { delta ->
                        val next =
                            if (range == null) {
                                LaimoryTimePickerMath.scrollMinute(value, delta, minuteStep, dateOptions)
                            } else {
                                val minute = LaimoryTimePickerMath.scrollWithin(minuteOptions, value.time.minute, delta)
                                LaimoryTimePickerMath.coerceIntoRange(
                                    value.copy(time = value.time.withMinute(minute)),
                                    range,
                                )
                            }
                        onValueChange(next, TimePickerColumn.MINUTE)
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

/** 가운데 행을 감싸는 위·아래 경계선. 어느 행이 선택 중인지 시각적으로 고정한다. */
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
    textStyle: TextStyle,
    onDelta: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    val cycles = if (isCyclic) CYCLIC_REPEAT_COUNT else 1
    val itemCount = options.size * cycles
    val baseIndex = if (isCyclic) options.size * (cycles / 2) else 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = baseIndex + selectedIndex)
    // 목록 구성이 바뀌면 이동량 기준점도 다시 잡는다.
    var lastIndex by remember(options.size) { mutableIntStateOf(baseIndex + selectedIndex) }

    // 가운데 칸에 놓인 항목. 강조와 값 모두 이 값을 쓴다.
    //
    // firstVisibleItemIndex를 그대로 쓰면 두 군데서 어긋난다. 하나는 스냅 도중 — 한 행을 온전히 지나야
    // 바뀌는데 스냅은 반 행에서 넘어가므로 강조가 늦게 따라온다. 다른 하나는 목록 끝 — 뷰포트 높이와
    // 행 높이의 픽셀 반올림 차이 때문에 마지막 항목은 몇 픽셀 모자라게 멈춰, 가운데 보이는 항목과
    // firstVisibleItemIndex가 영영 한 칸 어긋난 채로 남는다(23을 가리키는데 값은 22가 되는 식).
    val rowHeightPx = with(LocalDensity.current) { RollerRowHeight.roundToPx() }
    val centerIndexState =
        remember(listState, rowHeightPx, itemCount) {
            derivedStateOf {
                val index = listState.firstVisibleItemIndex
                val hasPassedHalfRow = listState.firstVisibleItemScrollOffset * 2 >= rowHeightPx
                if (hasPassedHalfRow) (index + 1).coerceAtMost(itemCount - 1) else index
            }
        }
    val centerIndex by centerIndexState

    // 스크롤이 아닌 경로(다른 열의 자정 carry 등)로 값이 바뀌면 롤러도 따라 이동한다.
    LaunchedEffect(selectedIndex, options.size) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        val target = LaimoryTimePickerMath.nearestIndexOf(selectedIndex, lastIndex, options.size, itemCount)
        if (centerIndexState.value != target) {
            // 기준점을 먼저 옮겨 이 이동이 다시 onDelta로 되먹임되지 않게 한다.
            lastIndex = target
            listState.animateScrollToItem(target)
        }
    }

    // 스크롤이 멈춘 뒤에만 이동량을 알려 중간 프레임마다 값이 튀지 않게 한다.
    //
    // 이 코루틴은 목록이 바뀌지 않는 한 다시 시작하지 않으므로, 콜백을 직접 붙잡으면 처음 컴포지션의
    // 람다를 계속 쓰게 된다. 그 람다는 처음 값을 캡처하고 있어 이동량이 매번 처음 값에 더해진다.
    val currentOnDelta by rememberUpdatedState(onDelta)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    LaunchedEffect(listState, options.size) {
        snapshotFlow { listState.isScrollInProgress to centerIndexState.value }
            .filter { (scrolling, _) -> !scrolling }
            .map { (_, index) -> index }
            .distinctUntilChanged()
            .collect { index ->
                val delta = index - lastIndex
                lastIndex = index
                if (delta != 0) currentOnDelta(delta)

                // 화면이 이동을 받아들이지 않고 같은 값으로 되돌리면(허용 범위 밖일 때) 상태가 그대로라
                // 아래 재배치 이펙트가 다시 돌지 않는다. 그러면 롤러만 움직인 채로 값과 어긋난다.
                // 값이 반영될 틈을 준 뒤, 값이 가리키는 자리로 직접 맞춘다.
                delay(VALUE_ECHO_GRACE_MILLIS)
                if (listState.isScrollInProgress) return@collect
                val settled =
                    LaimoryTimePickerMath.nearestIndexOf(
                        currentSelectedIndex,
                        lastIndex,
                        options.size,
                        itemCount,
                    )
                if (centerIndexState.value != settled) {
                    lastIndex = settled
                    listState.animateScrollToItem(settled)
                }
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
        items(count = itemCount) { index ->
            val option = options[index % options.size]
            val isSelected = index == centerIndex
            Box(
                modifier = Modifier.fillMaxWidth().height(RollerRowHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    modifier = if (isSelected) Modifier else Modifier.alpha(UNSELECTED_ROW_ALPHA),
                    // 값이 바뀔 때마다 숫자 폭이 달라지면 열이 좌우로 흔들린다.
                    style = textStyle.tabularFigures(),
                    color =
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** 롤러 밖으로 새어 나가는 세로 스크롤·fling을 삼켜 바깥 스크롤 영역이 함께 움직이지 않게 한다. */
private class RollerScrollBoundary : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = Offset(x = 0f, y = available.y)

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity = Velocity(x = 0f, y = available.y)
}

/** 이동량을 알린 뒤 화면의 값이 되돌아오길 기다리는 시간. 지나면 값 쪽으로 자리를 맞춘다. */
private const val VALUE_ECHO_GRACE_MILLIS = 250L

/** 롤러에 보이는 행 수(위·가운데·아래). */
private const val VISIBLE_ROW_COUNT = 3
private const val UNSELECTED_ROW_ALPHA = 0.3f
private const val CYCLIC_REPEAT_COUNT = 201

private val RollerRowHeight = 140.dp / VISIBLE_ROW_COUNT
internal val RollerHeight = RollerRowHeight * VISIBLE_ROW_COUNT
