package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.component.EmotionIcon
import com.soma369.laimory.core.ui.component.EmotionIconDefaults
import com.soma369.laimory.core.ui.model.displayLabel
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.timeline.model.CALENDAR_FIRST_MONTH
import com.soma369.laimory.feature.timeline.model.CALENDAR_LAST_MONTH
import com.soma369.laimory.feature.timeline.model.CalendarMonthGrid
import com.soma369.laimory.feature.timeline.model.CalendarRecordUiModel
import com.soma369.laimory.feature.timeline.model.DAYS_IN_WEEK
import com.soma369.laimory.feature.timeline.model.MONTH_PAGE_COUNT
import com.soma369.laimory.feature.timeline.model.monthOfPagerPage
import com.soma369.laimory.feature.timeline.model.toCalendarMonthGrid
import com.soma369.laimory.feature.timeline.model.toPagerPage
import com.soma369.laimory.feature.timeline.state.MonthlyRecordsUiContent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

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
 * 월간 격자를 좌우로 넘기는 pager.
 *
 * 드래그가 손가락을 따라오고 손을 떼면 가까운 달로 정착한다. 페이지 번호와 월은
 * [toPagerPage]·[monthOfPagerPage] 로 1:1 대응하므로 앵커 상태를 따로 들지 않는다.
 * 정착한 페이지만 [onVisibleMonthChange] 로 올려 드래그 중간 값이 상태에 새지 않게 한다.
 *
 * 조회 중·실패는 pager 를 걷어내지 않고 [months] 를 따라 페이지 안에서 표현한다. 화면 전체를 로딩으로
 * 바꾸면 pager 가 컴포지션에서 빠져 스와이프가 끊기고 정착 페이지가 다시 발행된다.
 *
 * 스와이프는 TalkBack 에 노출되지 않으므로 같은 이동을 접근성 커스텀 액션으로도 제공한다.
 */
@Composable
internal fun CalendarMonthPager(
    visibleMonth: YearMonth,
    months: Map<YearMonth, MonthlyRecordsUiContent>,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onVisibleMonthChange: (YearMonth) -> Unit,
    onRetryMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = visibleMonth.toPagerPage()) { MONTH_PAGE_COUNT }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page -> onVisibleMonthChange(monthOfPagerPage(page)) }
    }
    // 연·월 피커처럼 pager 밖에서 월이 바뀌면 따라 맞춘다.
    LaunchedEffect(visibleMonth) {
        val target = visibleMonth.toPagerPage()
        if (target == pagerState.currentPage) return@LaunchedEffect
        // 옆 달은 넘어가는 게 보이도록 애니메이션하고, 멀리 뛸 때는 즉시 옮긴다.
        if (abs(target - pagerState.currentPage) <= 1) {
            pagerState.animateScrollToPage(target)
        } else {
            pagerState.scrollToPage(target)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier =
            modifier.fillMaxWidth().semantics {
                // 경계 밖으로는 움직일 수 없으므로 액션도 내놓지 않는다. 남겨 두면 눌러도 그대로인데
                // TalkBack 에는 이동에 성공했다고 전달된다.
                customActions =
                    buildList {
                        if (visibleMonth.isAfter(CALENDAR_FIRST_MONTH)) {
                            add(
                                CustomAccessibilityAction("이전 달 보기") {
                                    onVisibleMonthChange(visibleMonth.minusMonths(1))
                                    true
                                },
                            )
                        }
                        if (visibleMonth.isBefore(CALENDAR_LAST_MONTH)) {
                            add(
                                CustomAccessibilityAction("다음 달 보기") {
                                    onVisibleMonthChange(visibleMonth.plusMonths(1))
                                    true
                                },
                            )
                        }
                    }
            },
    ) { page ->
        val month = monthOfPagerPage(page)
        when (val content = months[month] ?: MonthlyRecordsUiContent.Loading) {
            MonthlyRecordsUiContent.Loading -> CalendarMonthLoading()
            MonthlyRecordsUiContent.LoadFailed -> CalendarMonthLoadFailed(onRetryClick = { onRetryMonth(month) })
            is MonthlyRecordsUiContent.Records ->
                CalendarMonthGridView(
                    grid = remember(month) { month.toCalendarMonthGrid() },
                    recordsByDate = content.recordsByDate,
                    selectedDate = selectedDate,
                    today = today,
                    onSelectDate = onSelectDate,
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}

/** 내용이 아직 없는 달. 격자 자리를 그대로 차지해 페이지를 넘기는 동안 화면 높이가 흔들리지 않는다. */
@Composable
private fun CalendarMonthLoading() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                // 진행 표시는 그림이라 TalkBack 이 읽을 것이 없다. 어떤 상태인지 문구로 알린다.
                .semantics { contentDescription = "이 달을 불러오는 중" },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** 내용 없이 실패한 달. 화면 전체가 아니라 이 달만 다시 시도한다. */
@Composable
private fun CalendarMonthLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "이 달의 기록을 불러오지 못했어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onRetryClick,
            modifier = Modifier.padding(top = Spacing.medium),
        ) {
            Text("다시 시도")
        }
    }
}

/** 한 달치 날짜 격자. */
@Composable
internal fun CalendarMonthGridView(
    grid: CalendarMonthGrid,
    recordsByDate: Map<LocalDate, CalendarRecordUiModel>,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
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
                .semantics {
                    contentDescription = dayCellDescription(date, record, isToday)
                    // 선택 날짜는 테두리·색으로만 구분돼 접근성 서비스에는 보이지 않는다.
                    selected = isSelected
                },
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
            if (record == null) "기록 없음" else "기록 있음, 감정 ${record.emotion.displayLabel()}",
        )
    }

private val WEEK_DAYS: List<DayOfWeek> = List(DAYS_IN_WEEK) { index -> DayOfWeek.SUNDAY.plus(index.toLong()) }

private val DayNumberBoxSize = 32.dp
private val CellBorderWidth = 0.5.dp
private val SelectedCellBorderWidth = 1.5.dp
private val SelectedCellCornerRadius = 8.dp
private const val CELL_BORDER_ALPHA = 0.5f
