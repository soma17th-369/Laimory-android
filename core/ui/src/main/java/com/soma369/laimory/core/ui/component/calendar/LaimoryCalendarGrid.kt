package com.soma369.laimory.core.ui.component.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimoryColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * 일요일 시작 요일 헤더.
 *
 * 주말만 색으로 구분한다 — 일요일은 error 계열, 토요일은 info 계열이다. 공휴일 색은 정본
 * 데이터가 없어 범위 밖이다.
 */
@Composable
fun CalendarWeekdayHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        WEEK_DAYS.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.KOREA),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color =
                    when (dayOfWeek) {
                        DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.error
                        DayOfWeek.SATURDAY -> MaterialTheme.laimoryColors.info
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 한 달치 날짜 격자. **기록을 모른다.**
 *
 * 아는 것은 표시 월·선택 날짜·오늘·고를 수 없는 날짜까지다. 감정 색이나 초안·저장 상태처럼 날짜에
 * 무엇이 있는지는 [decoration] 과 [dayDetail] 로 호출부가 얹는다 — 격자를 쓰는 곳마다 그 뜻이 달라
 * 격자가 알면 쓰는 곳이 하나로 묶인다.
 *
 * @param selectedDate 테두리로 표시할 날짜. 고른 것이 없으면 null.
 * @param isSelectable 고를 수 있는 날짜인지. false 인 날은 흐리게 그리고 눌리지 않는다.
 * @param dayDetail 셀 하나의 낭독 문구와 클릭 라벨. 격자는 날짜·오늘 여부까지만 읽는다.
 * @param decoration 날짜 아래 장식 자리. **모든 날에 같은 높이로** 그려야 셀 높이가 흔들리지 않는다.
 */
@Composable
fun LaimoryCalendarGrid(
    grid: CalendarMonthGrid,
    selectedDate: LocalDate?,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isSelectable: (LocalDate) -> Boolean = { true },
    dayDetail: (LocalDate) -> CalendarDayDetail = { CalendarDayDetail() },
    decoration: @Composable (LocalDate) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 주 사이를 띄우지 않는다. Figma 는 행 간격 2px 을 선언하지만 셀 테두리가 그 틈을 거의 메워
        // 렌더 결과는 열 경계와 같은 연속된 hairline 이다. 간격을 그대로 옮기면 배경이 드러나
        // 가로선만 끊어져 보인다.
        grid.weeks.forEach { week ->
            Row(modifier = Modifier.weight(1f)) {
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        isSelected = date != null && date == selectedDate,
                        isToday = date != null && date == today,
                        isEnabled = date != null && isSelectable(date),
                        detail = date?.let(dayDetail) ?: CalendarDayDetail(),
                        onClick = onSelectDate,
                        decoration = decoration,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    isEnabled: Boolean,
    detail: CalendarDayDetail,
    onClick: (LocalDate) -> Unit,
    decoration: @Composable (LocalDate) -> Unit,
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
            CalendarDayCellContent(date = null, isSelected = false, isEnabled = true, decoration = decoration)
        }
        return
    }

    Box(
        modifier =
            cellModifier
                .clickable(enabled = isEnabled, onClickLabel = detail.clickLabel) { onClick(date) }
                .semantics {
                    contentDescription = dayCellDescription(date, isToday, isEnabled, detail.note)
                    // 선택 날짜는 테두리·색으로만 구분돼 접근성 서비스에는 보이지 않는다.
                    selected = isSelected
                    // 눌리지 않는 날은 색만 흐릴 뿐이라 낭독에는 드러나지 않는다.
                    if (!isEnabled) disabled()
                },
    ) {
        CalendarDayCellContent(date = date, isSelected = isSelected, isEnabled = isEnabled, decoration = decoration)
    }
}

@Composable
private fun CalendarDayCellContent(
    date: LocalDate?,
    isSelected: Boolean,
    isEnabled: Boolean,
    decoration: @Composable (LocalDate) -> Unit,
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
                        when {
                            !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_DAY_ALPHA)
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
        if (date != null) decoration(date)
    }
}

/** TalkBack 이 셀 하나를 한 문장으로 읽도록 날짜·오늘 여부·호출부가 준 말을 합친다. */
private fun dayCellDescription(
    date: LocalDate,
    isToday: Boolean,
    isEnabled: Boolean,
    note: String?,
): String =
    buildString {
        append("${date.monthValue}월 ${date.dayOfMonth}일 ")
        append(date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREA))
        if (isToday) append(", 오늘")
        if (!isEnabled) append(", 선택할 수 없음")
        if (note != null) append(", $note")
    }

private val WEEK_DAYS: List<DayOfWeek> = List(DAYS_IN_WEEK) { index -> DayOfWeek.SUNDAY.plus(index.toLong()) }

private val DayNumberBoxSize = 32.dp
private val CellBorderWidth = 0.5.dp
private val SelectedCellBorderWidth = 1.5.dp
private val SelectedCellCornerRadius = 8.dp
private const val CELL_BORDER_ALPHA = 0.5f
private const val DISABLED_DAY_ALPHA = 0.38f

@Preview(name = "달력 격자 · 라이트", showBackground = true)
@Preview(name = "달력 격자 · 다크", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LaimoryCalendarGridPreview() {
    val month = YearMonth.of(2026, 9)
    val today = month.atDay(11)
    LaimoryTheme {
        Column(modifier = Modifier.padding(Spacing.large)) {
            CalendarWeekdayHeader()
            LaimoryCalendarGrid(
                grid = month.toCalendarMonthGrid(),
                selectedDate = month.atDay(4),
                today = today,
                onSelectDate = {},
                modifier = Modifier.height(PreviewGridHeight),
                isSelectable = { !it.isAfter(today) },
            )
        }
    }
}

private val PreviewGridHeight = 300.dp
