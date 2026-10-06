package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import com.soma369.laimory.core.ui.component.calendar.CalendarDayDetail
import com.soma369.laimory.core.ui.component.calendar.CalendarWeekdayHeader
import com.soma369.laimory.core.ui.component.calendar.LaimoryCalendarGrid
import com.soma369.laimory.core.ui.component.calendar.toCalendarMonthGrid
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimoryColors
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomeDatePickerSession
import com.soma369.laimory.feature.home.state.isSelectableRecordDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import com.soma369.laimory.core.ui.R as UiR

/**
 * 타임라인을 만들 날짜와 기록 범위를 함께 고르는 다이얼로그.
 *
 * 캘린더 탭과 **같은 격자**를 쓴다. 같은 앱에서 날짜를 고르는 자리가 두 가지 생김새이면 어느
 * 쪽이 우리 달력인지 알 수 없다.
 *
 * 자주 고르는 오늘·어제는 칩으로 한 번에 간다. 칩으로 확정까지 하지는 않는다 — 격자 탭과
 * 동작이 갈리면 어느 쪽이 바로 닫히는지 눌러 봐야 안다.
 *
 * 기록이 있는 날은 날짜 아래 도트로 알린다 — 저장 완료는 `primary`, 아직 저장하지 않은 초안은
 * `secondary`. 색만으로 가르지 않고 낭독 문구에 `작성 완료`·`작성 중` 을 함께 담는다.
 *
 * 고르는 동안의 날짜·범위는 [session] 이 들고 있다(ViewModel 소유). 이 다이얼로그는 보이는 달만 기억한다.
 *
 * @param savedDates 저장이 끝난 기록의 날짜. 아직 받지 못한 달은 비어 있다.
 * @param draftDates 아직 저장하지 않은 초안의 날짜. 받는 범위는 [savedDates] 와 같다.
 * @param retentionDays 수집 보존 일수. 오늘을 포함해 이만큼만 고를 수 있다. null 이면 제한하지 않는다.
 * @param defaultRangeLabel 기기에 저장된 기본 범위(`06:00 ~ 익일 06:00`). 지금 범위가 이와 다르면 칩 아래에
 *   `이 날만 · 기본 …` 으로 알린다.
 */
@Composable
internal fun HomeDatePickerDialog(
    session: HomeDatePickerSession,
    savedDates: Set<LocalDate>,
    draftDates: Set<LocalDate>,
    retentionDays: Int?,
    onPickDate: (LocalDate) -> Unit,
    onRangeClick: () -> Unit,
    isOneDayRange: Boolean,
    defaultRangeLabel: String,
    onConfirm: () -> Unit,
    onDisplayedMonthChange: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    // 기록 창과 같은 시간대의 오늘이다. 다이얼로그가 열려 있는 동안만 사는 값이라 자정을 넘겨도
    // 어긋나지 않는다.
    val today = remember { LocalDate.now() }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(session.date)) }

    // 달을 넘길 때마다 그 달의 기록을 받는다. 열자마자 현재 값이 한 번 흘러 첫 달도 받는다.
    LaunchedEffect(visibleMonth) { onDisplayedMonthChange(visibleMonth) }

    // 보존 기간 밖은 기기의 재료가 이미 지워져 만들어도 빈 초안이 된다. 기록이 있는 날은 막지 않는다 —
    // 고르면 CTA 가 `타임라인 확인하기` 로 그 기록을 연다. 달을 넘겨 보는 것은 막지 않는다.
    val isSelectable: (LocalDate) -> Boolean = { date -> isSelectableRecordDate(date, today, retentionDays) }
    val pickDate: (LocalDate) -> Unit = { date ->
        onPickDate(date)
        visibleMonth = YearMonth.from(date)
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
                // 작은 화면에서는 이 영역만 스크롤한다. 취소·확인 줄은 아래에 고정해 손가락 밑에서 움직이지 않는다.
                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
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
                            isSelected = session.date == today,
                            isEnabled = isSelectable(today),
                            onClick = { pickDate(today) },
                        )
                        QuickDateChip(
                            label = "어제",
                            date = today.minusDays(1),
                            isSelected = session.date == today.minusDays(1),
                            isEnabled = isSelectable(today.minusDays(1)),
                            onClick = { pickDate(today.minusDays(1)) },
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
                        selectedDate = session.date,
                        today = today,
                        onSelectDate = pickDate,
                        modifier = Modifier.height(DayCellHeight * CALENDAR_MAX_WEEKS),
                        isSelectable = isSelectable,
                        dayDetail = { date -> recordDayDetail(isSaved = date in savedDates, isDraft = date in draftDates) },
                        decoration = { date ->
                            RecordDot(
                                isSaved = date in savedDates,
                                isDraft = date in draftDates,
                                isEnabled = isSelectable(date),
                            )
                        },
                    )
                    RecordRangeSection(
                        session = session,
                        onRangeClick = onRangeClick,
                        isOneDayRange = isOneDayRange,
                        defaultRangeLabel = defaultRangeLabel,
                    )
                }
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
                        onClick = onConfirm,
                        enabled = isSelectable(session.date),
                    ) {
                        Text("확인")
                    }
                }
            }
        }
    }
}

/**
 * 격자 아래 기록 범위 영역(Figma 3119:12942).
 *
 * - 1행: `기록 범위` ⓘ · 범위 칩(양 끝)
 * - 2행: 범위가 저장된 기본값과 다를 때만 `이 날만 · 기본 06:00 ~ 익일 06:00`(오른쪽 정렬)
 *
 * 기본값으로 쓸지는 칩을 눌러 연 시간 시트에서 고른다(`항상 이 시간으로` / `이 날만`). 여기서는 지금 범위가
 * 어느 쪽인지만 보여 준다 — 저장된 기본값이 적용되는 자리에 보이지 않으면 왜 다른 날짜에서 범위가 바뀌는지
 * 알 수 없다. 2행은 비어 있어도 자리를 지킨다. 범위가 바뀔 때마다 높이가 달라지면 확인 버튼이 움직인다.
 *
 * 범위는 홈 카드의 데이터를 거르는 필터라 어느 날이든 바꿀 수 있다.
 */
@Composable
private fun RecordRangeSection(
    session: HomeDatePickerSession,
    onRangeClick: () -> Unit,
    isOneDayRange: Boolean,
    defaultRangeLabel: String,
) {
    Column(modifier = Modifier.padding(top = Spacing.large)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "기록 범위",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                RecordRangeInfo()
            }
            RangeChip(label = session.timeRangeLabel(), onClick = onRangeClick)
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.extraSmall)
                    .alpha(if (isOneDayRange) 1f else 0f)
                    .clearAndSetSemantics {
                        if (isOneDayRange) contentDescription = "이 날만 쓰는 범위. 기본 $defaultRangeLabel"
                    },
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "이 날만",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.laimoryColors.primaryText,
            )
            Text(
                text = "· 기본 $defaultRangeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 기록 범위 안내. 누르면 툴팁으로 보여 주고, 바깥을 누르면 닫힌다. 날짜 피커와 시간 시트가 같은 설명을 쓴다 —
 * 어디서 눌러도 같은 답이 나와야 한다.
 *
 * 시안대로 아이콘 둘레 4 만 둔다(누르는 영역 24). 낭독 문구에 설명을 그대로 담는다 — 스크린 리더 사용자는
 * 툴팁을 열지 않아도 듣는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordRangeInfo() {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(RECORD_RANGE_GUIDE) } },
        state = tooltipState,
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ico_setting_info),
            contentDescription = "기록 범위 안내: $RECORD_RANGE_GUIDE",
            modifier =
                Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { scope.launch { tooltipState.show() } }
                    .padding(Spacing.extraSmall)
                    .size(RangeInfoIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val RECORD_RANGE_GUIDE = "이 시간 안에 모인 사진·일정·위치·알림으로 타임라인을 만들어요.\n6시간 이상 · 종료는 익일 06:00까지"

/** 지금 범위 + 캐럿. 누르면 시간 시트가 다이얼로그 위에 뜬다. */
@Composable
private fun RangeChip(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics { role = Role.Button },
        shape = RoundedCornerShape(percent = CHIP_CORNER_PERCENT),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(start = Spacing.medium, end = Spacing.small, top = Spacing.small, bottom = Spacing.small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                painter = painterResource(UiR.drawable.ico_default_caret_down),
                contentDescription = null,
                modifier = Modifier.size(RangeCaretSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 날짜 아래 기록 도트. 기록이 없는 날도 **같은 자리를 비워 둔다** — 날짜마다 셀 높이가 흔들리지 않게 한다.
 *
 * 고를 수 없는 날은 날짜 숫자처럼 흐리게 그린다. 선명하면 눌릴 것처럼 보인다.
 */
@Composable
private fun RecordDot(
    isSaved: Boolean,
    isDraft: Boolean,
    isEnabled: Boolean,
) {
    val color =
        when {
            isSaved -> MaterialTheme.colorScheme.primary
            isDraft -> MaterialTheme.colorScheme.secondary
            else -> Color.Transparent
        }
    Box(
        modifier =
            Modifier
                .size(RecordDotSize)
                .background(if (isEnabled) color else color.copy(alpha = color.alpha * DISABLED_DOT_ALPHA), CircleShape),
    )
}

/** 도트는 색으로만 갈리므로 낭독에는 말로 옮긴다. */
private fun recordDayDetail(
    isSaved: Boolean,
    isDraft: Boolean,
): CalendarDayDetail =
    CalendarDayDetail(
        note =
            when {
                isSaved -> "작성 완료"
                isDraft -> "작성 중"
                else -> null
            },
    )

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

/**
 * 날짜 한 칸의 높이. 숫자(32)와 도트(6)에 격자의 위아래 여백(4+4)·둘 사이 간격(8)을 더한 값이다 —
 * 모자라면 도트가 셀 아래로 잘린다.
 */
private val DayCellHeight = 54.dp
private val RecordDotSize = 6.dp

/** 고를 수 없는 날의 도트. 격자가 날짜 숫자를 흐리게 그리는 비율과 같다. */
private const val DISABLED_DOT_ALPHA = 0.38f
private val StepperTouchTarget = 44.dp
private val RangeCaretSize = 16.dp
private val RangeInfoIconSize = 16.dp

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
                session =
                    HomeDatePickerSession(
                        date = today.minusDays(1),
                        startTime = LocalTime.of(5, 0),
                        endDay = DraftEndDay.NEXT_DAY,
                        endTime = LocalTime.of(6, 0),
                    ),
                savedDates = setOf(today.minusDays(3)),
                draftDates = setOf(today.minusDays(1)),
                retentionDays = 30,
                onPickDate = {},
                onRangeClick = {},
                isOneDayRange = true,
                defaultRangeLabel = "06:00 ~ 익일 06:00",
                onConfirm = {},
                onDisplayedMonthChange = {},
                onDismiss = {},
            )
        }
    }
}
