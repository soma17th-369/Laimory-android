package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soma369.laimory.core.ui.component.calendar.CALENDAR_FIRST_MONTH
import com.soma369.laimory.core.ui.component.calendar.CALENDAR_LAST_MONTH
import com.soma369.laimory.core.ui.component.calendar.CALENDAR_MAX_WEEKS
import com.soma369.laimory.core.ui.component.calendar.CalendarWeekdayHeader
import com.soma369.laimory.core.ui.component.calendar.LaimoryCalendarGrid
import com.soma369.laimory.core.ui.component.calendar.toCalendarMonthGrid
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import java.time.LocalDate
import java.time.YearMonth
import com.soma369.laimory.core.ui.R as UiR

/**
 * 초안을 만들 날짜를 고르는 다이얼로그.
 *
 * 캘린더 탭과 **같은 격자**를 쓴다. 같은 앱에서 날짜를 고르는 자리가 두 가지 생김새이면 어느
 * 쪽이 우리 달력인지 알 수 없다.
 *
 * 자주 고르는 오늘·어제는 칩으로 한 번에 간다. 칩으로 확정까지 하지는 않는다 — 격자 탭과
 * 동작이 갈리면 어느 쪽이 바로 닫히는지 눌러 봐야 안다.
 *
 * @param savedDates 이미 저장이 끝나 고를 수 없는 날짜. 아직 받지 못한 달은 비어 있다.
 */
@Composable
internal fun HomeDatePickerDialog(
    initialDate: LocalDate,
    savedDates: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
    onDisplayedMonthChange: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    // 기록 창과 같은 시간대의 오늘이다. 다이얼로그가 열려 있는 동안만 사는 값이라 자정을 넘겨도
    // 어긋나지 않는다.
    val today = remember { LocalDate.now() }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    // 달을 넘길 때마다 그 달의 저장 날짜를 받는다. 열자마자 현재 값이 한 번 흘러 첫 달도 받는다.
    LaunchedEffect(visibleMonth) { onDisplayedMonthChange(visibleMonth) }

    val isSelectable: (LocalDate) -> Boolean = { date ->
        // 저장이 끝난 날짜는 서버가 초안 생성을 409 로 거절한다. 고르게 두면 사진까지 다 고른
        // 뒤 마지막에야 막힌다. 초안 날짜는 막지 않는다 — 서버가 이어 붙이기로 받아 준다.
        !date.isAfter(today) && date !in savedDates
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(Spacing.extraLarge2).widthIn(max = DialogMaxWidth),
            shape = RoundedCornerShape(Spacing.extraLarge2),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(Spacing.extraLarge)) {
                Text(
                    text = "타임라인을 만들 날짜",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.padding(top = Spacing.large),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    QuickDateChip(
                        label = "오늘",
                        date = today,
                        isSelected = selectedDate == today,
                        isEnabled = isSelectable(today),
                        onClick = {
                            selectedDate = today
                            visibleMonth = YearMonth.from(today)
                        },
                    )
                    QuickDateChip(
                        label = "어제",
                        date = today.minusDays(1),
                        isSelected = selectedDate == today.minusDays(1),
                        isEnabled = isSelectable(today.minusDays(1)),
                        onClick = {
                            selectedDate = today.minusDays(1)
                            visibleMonth = YearMonth.from(today.minusDays(1))
                        },
                    )
                }
                MonthStepper(
                    visibleMonth = visibleMonth,
                    onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                    onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                )
                CalendarWeekdayHeader(modifier = Modifier.padding(bottom = Spacing.extraSmall))
                // 달마다 4~6주로 달라지는 것을 늘 6주로 편다. 그러지 않으면 달을 넘길 때마다
                // 다이얼로그 높이가 달라져 아래 버튼이 손가락 밑에서 움직인다.
                val grid = remember(visibleMonth) { visibleMonth.toCalendarMonthGrid(minWeeks = CALENDAR_MAX_WEEKS) }
                LaimoryCalendarGrid(
                    grid = grid,
                    selectedDate = selectedDate,
                    today = today,
                    onSelectDate = { selectedDate = it },
                    modifier = Modifier.height(DayCellHeight * CALENDAR_MAX_WEEKS),
                    isSelectable = isSelectable,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.large),
                    horizontalArrangement = Arrangement.End,
                ) {
                    // 되돌리는 쪽과 확정하는 쪽을 색으로 가른다. 같은 색이면 어느 것이 진행인지
                    // 문구를 읽어야 안다.
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    ) {
                        Text("취소")
                    }
                    TextButton(
                        onClick = { onSelect(selectedDate) },
                        // 조회가 끝나기 전에 고른 날짜가 뒤늦게 저장됨으로 판정될 수 있다.
                        enabled = isSelectable(selectedDate),
                    ) {
                        Text("확인")
                    }
                }
            }
        }
    }
}

/** 자주 고르는 하루로 한 번에 가는 칩. 그 날짜를 고를 수 없으면 칩도 눌리지 않는다. */
@Composable
private fun QuickDateChip(
    label: String,
    date: LocalDate,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier =
            Modifier.semantics {
                role = Role.RadioButton
                // 선택된 칩은 색으로만 구분돼 접근성 서비스에는 보이지 않는다.
                selected = isSelected
            },
        shape = RoundedCornerShape(percent = CHIP_CORNER_PERCENT),
        color =
            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor =
            if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
            ),
    ) {
        Text(
            // 칩 문구만으로는 어느 날인지 모른다. 날짜를 함께 적어 격자에서 찾지 않게 한다.
            text = "$label · ${date.monthValue}/${date.dayOfMonth}",
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** `‹ 2026년 9월 ›`. 다룰 수 있는 범위 끝에서는 그쪽 버튼이 꺼진다. */
@Composable
private fun MonthStepper(
    visibleMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(StepperTouchTarget),
            enabled = visibleMonth.isAfter(CALENDAR_FIRST_MONTH),
        ) {
            // tint 를 직접 주면 IconButton 의 disabled content color 를 덮어써 비활성이 활성처럼 보인다.
            Icon(
                painter = painterResource(UiR.drawable.ico_default_caret_left),
                contentDescription = "이전 달 보기",
                modifier = Modifier.size(StepperIconSize),
            )
        }
        Text(
            text = "${visibleMonth.year}년 ${visibleMonth.monthValue}월",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(StepperTouchTarget),
            enabled = visibleMonth.isBefore(CALENDAR_LAST_MONTH),
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ico_default_caret_right),
                contentDescription = "다음 달 보기",
                modifier = Modifier.size(StepperIconSize),
            )
        }
    }
}

private val DialogMaxWidth = 360.dp
private val DayCellHeight = 48.dp
private val StepperTouchTarget = 44.dp
private val StepperIconSize = 20.dp
private const val CHIP_CORNER_PERCENT = 50

@Preview(name = "홈 날짜 피커 · 라이트", showBackground = true)
@Preview(
    name = "홈 날짜 피커 · 다크",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeDatePickerDialogPreview() {
    val today = LocalDate.of(2026, 9, 11)
    LaimoryTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            HomeDatePickerDialog(
                initialDate = today.minusDays(1),
                savedDates = setOf(today.minusDays(3)),
                onSelect = {},
                onDisplayedMonthChange = {},
                onDismiss = {},
            )
        }
    }
}
