package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.component.EmotionIcon
import com.soma369.laimory.core.ui.component.EmotionIconDefaults
import com.soma369.laimory.core.ui.theme.Emotion
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.timeline.model.CalendarMonthGrid
import com.soma369.laimory.feature.timeline.model.CalendarRecordUiModel
import com.soma369.laimory.feature.timeline.model.DAYS_IN_WEEK
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** 일요일 시작 요일 헤더. 일요일만 error 계열로 구분한다(공휴일 색은 정본 데이터가 없어 범위 밖). */
@Composable
internal fun CalendarWeekdayHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        WEEK_DAYS.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.KOREA),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color =
                    if (dayOfWeek == DayOfWeek.SUNDAY) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 월간 날짜 격자.
 *
 * 좌우 스와이프로 월을 넘긴다. 스와이프는 TalkBack 에 노출되지 않으므로 같은 동작을 접근성
 * 커스텀 액션으로도 제공한다.
 */
@Composable
internal fun CalendarMonthGridView(
    grid: CalendarMonthGrid,
    recordsByDate: Map<LocalDate, CalendarRecordUiModel>,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .pointerInput(onPreviousMonth, onNextMonth) {
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = {
                            when {
                                dragged >= MonthSwipeThreshold.toPx() -> onPreviousMonth()
                                dragged <= -MonthSwipeThreshold.toPx() -> onNextMonth()
                            }
                        },
                    ) { change, dragAmount ->
                        dragged += dragAmount
                        change.consume()
                    }
                }.semantics {
                    customActions =
                        listOf(
                            CustomAccessibilityAction("이전 달 보기") {
                                onPreviousMonth()
                                true
                            },
                            CustomAccessibilityAction("다음 달 보기") {
                                onNextMonth()
                                true
                            },
                        )
                },
    ) {
        // 주 사이를 띄우지 않는다. Figma 는 행 간격 2px 을 선언하지만 셀 테두리가 그 틈을 거의 메워
        // 렌더 결과는 열 경계와 같은 연속된 hairline 이다. 간격을 그대로 옮기면 배경이 드러나
        // 가로선만 끊어져 보인다.
        grid.weeks.forEach { week ->
            Row(modifier = Modifier.weight(1f)) {
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        record = date?.let(recordsByDate::get),
                        isSelected = date != null && date == selectedDate,
                        isToday = date != null && date == today,
                        onClick = onSelectDate,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDayCell(
    date: LocalDate?,
    record: CalendarRecordUiModel?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: (LocalDate) -> Unit,
) {
    val shape = if (isSelected) RoundedCornerShape(SelectedCellCornerRadius) else RectangleShape
    val cellModifier =
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier.border(SelectedCellBorderWidth, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier.border(
                        CellBorderWidth,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = CELL_BORDER_ALPHA),
                    )
                },
            )

    if (date == null) {
        // 이웃 월 자리는 격자 선만 유지하고 접근성 트리에서 제외한다.
        Box(modifier = cellModifier.clearAndSetSemantics { }) {
            CalendarDayCellContent(date = null, record = null, isSelected = false)
        }
        return
    }

    Box(
        modifier =
            cellModifier
                .clickable(onClickLabel = if (record != null) "기록 열기" else "날짜 선택") { onClick(date) }
                .semantics { contentDescription = dayCellDescription(date, record, isToday) },
    ) {
        CalendarDayCellContent(date = date, record = record, isSelected = isSelected)
    }
}

@Composable
private fun CalendarDayCellContent(
    date: LocalDate?,
    record: CalendarRecordUiModel?,
    isSelected: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.extraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Box(
            modifier = Modifier.size(DayNumberBoxSize),
            contentAlignment = Alignment.Center,
        ) {
            if (date != null) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
        // 기록이 없는 날도 아이콘 자리를 비워 둔다 — 날짜마다 셀 높이가 달라지지 않게 한다.
        Box(
            modifier = Modifier.size(EmotionIconDefaults.Size),
            contentAlignment = Alignment.Center,
        ) {
            if (record != null) {
                EmotionIcon(emotion = record.emotion)
            }
        }
    }
}

/** TalkBack 이 셀 하나를 한 문장으로 읽도록 날짜·오늘 여부·기록 상태를 합친다. */
private fun dayCellDescription(
    date: LocalDate,
    record: CalendarRecordUiModel?,
    isToday: Boolean,
): String =
    buildString {
        append("${date.monthValue}월 ${date.dayOfMonth}일 ")
        append(date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREA))
        if (isToday) append(", 오늘")
        append(", ")
        append(
            when {
                record == null -> "기록 없음"
                record.emotion == null -> "기록 있음, 감정 미상"
                else -> "기록 있음, ${record.emotion.label()}"
            },
        )
    }

private fun Emotion.label(): String =
    when (this) {
        Emotion.JOY -> "활기"
        Emotion.CALM -> "평온"
        Emotion.MELLOW -> "무덤덤"
        Emotion.WEARY -> "지침"
        Emotion.DOWN -> "울적"
    }

private val WEEK_DAYS: List<DayOfWeek> = List(DAYS_IN_WEEK) { index -> DayOfWeek.SUNDAY.plus(index.toLong()) }

private val DayNumberBoxSize = 32.dp
private val CellBorderWidth = 0.5.dp
private val SelectedCellBorderWidth = 1.5.dp
private val SelectedCellCornerRadius = 8.dp
private val MonthSwipeThreshold = 48.dp
private const val CELL_BORDER_ALPHA = 0.5f
