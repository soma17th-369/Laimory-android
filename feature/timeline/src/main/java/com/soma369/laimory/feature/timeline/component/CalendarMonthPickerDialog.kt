package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import java.time.YearMonth
import com.soma369.laimory.core.ui.R as UiR

/**
 * TopBar 월 표기를 눌렀을 때 뜨는 연·월 피커.
 *
 * 월을 고르는 즉시 표시 월을 옮기고 닫는다 — 되돌릴 게 없는 이동이라 확인 버튼을 두지 않는다.
 * 연도는 피커 안에서만 넘어가므로, 월을 고르지 않고 닫으면 달력은 원래 월에 그대로 있다.
 */
@Composable
internal fun CalendarMonthPickerDialog(
    pickerYear: Int,
    visibleMonth: YearMonth,
    canShowPreviousYear: Boolean,
    canShowNextYear: Boolean,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // 창을 화면 전체로 늘렸기 때문에 플랫폼의 dismissOnClickOutside 가 동작하지 않는다
        // (바깥 터치 판정이 창 경계 기준이라 "바깥"이 존재하지 않는다). 빈 영역 탭을 직접 받는다.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
                    .semantics {
                        // 터치 바깥 닫기는 TalkBack 에 안 잡히므로 같은 동작을 액션으로 노출한다.
                        contentDescription = "연·월 선택 닫기"
                        onClick {
                            onDismiss()
                            true
                        }
                    }.padding(Spacing.extraLarge2),
            contentAlignment = Alignment.Center,
        ) {
            CalendarMonthPickerContent(
                pickerYear = pickerYear,
                visibleMonth = visibleMonth,
                canShowPreviousYear = canShowPreviousYear,
                canShowNextYear = canShowNextYear,
                onPreviousYear = onPreviousYear,
                onNextYear = onNextYear,
                onSelectMonth = onSelectMonth,
            )
        }
    }
}

@Composable
private fun CalendarMonthPickerContent(
    pickerYear: Int,
    visibleMonth: YearMonth,
    canShowPreviousYear: Boolean,
    canShowNextYear: Boolean,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = PickerMaxWidth)
                // 카드 안 여백을 눌렀을 때 바깥 닫기로 새지 않게 탭을 여기서 삼킨다.
                .pointerInput(Unit) { detectTapGestures { } },
        shape = RoundedCornerShape(PickerCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = PickerShadowElevation,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.extraLarge2),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
        ) {
            PickerYearStepper(
                pickerYear = pickerYear,
                canShowPreviousYear = canShowPreviousYear,
                canShowNextYear = canShowNextYear,
                onPreviousYear = onPreviousYear,
                onNextYear = onNextYear,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                (1..MONTHS_IN_YEAR).chunked(MONTHS_PER_ROW).forEach { rowMonths ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        rowMonths.forEach { month ->
                            val yearMonth = YearMonth.of(pickerYear, month)
                            PickerMonthCell(
                                month = month,
                                isSelected = yearMonth == visibleMonth,
                                onClick = { onSelectMonth(yearMonth) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerYearStepper(
    pickerYear: Int,
    canShowPreviousYear: Boolean,
    canShowNextYear: Boolean,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 서버가 받는 연도 범위 끝에서는 눌러도 반응하지 않는 대신 이동할 수 없음을 상태로 보여준다.
        IconButton(
            onClick = onPreviousYear,
            modifier = Modifier.size(StepperTouchTarget),
            enabled = canShowPreviousYear,
        ) {
            // tint 를 직접 주면 IconButton 의 disabled content color 를 덮어써 비활성 버튼이 활성처럼 보인다.
            Icon(
                painter = painterResource(UiR.drawable.ico_default_caret_left),
                contentDescription = "이전 해 보기",
                modifier = Modifier.size(StepperIconSize),
            )
        }
        Text(
            text = "${pickerYear}년",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        IconButton(
            onClick = onNextYear,
            modifier = Modifier.size(StepperTouchTarget),
            enabled = canShowNextYear,
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ico_default_caret_right),
                contentDescription = "다음 해 보기",
                modifier = Modifier.size(StepperIconSize),
            )
        }
    }
}

@Composable
private fun RowScope.PickerMonthCell(
    month: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .weight(1f)
                .height(MonthCellHeight)
                .semantics {
                    // 선택 월도 배경·글자색으로만 표현돼 접근성 서비스에는 보이지 않는다.
                    selected = isSelected
                    role = Role.RadioButton
                },
        shape = RoundedCornerShape(MonthCellCornerRadius),
        color =
            if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        border =
            if (isSelected) {
                null
            } else {
                BorderStroke(MonthCellBorderWidth, MaterialTheme.colorScheme.outlineVariant)
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "${month}월",
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}

private const val MONTHS_IN_YEAR = 12
private const val MONTHS_PER_ROW = 3

private val PickerMaxWidth = 320.dp
private val PickerCornerRadius = 24.dp
private val PickerShadowElevation = 8.dp
private val StepperTouchTarget = 48.dp
private val StepperIconSize = 24.dp
private val MonthCellHeight = 48.dp
private val MonthCellCornerRadius = 12.dp
private val MonthCellBorderWidth = 1.dp

@Preview(name = "연·월 피커", showBackground = true, widthDp = 360)
@Composable
private fun CalendarMonthPickerPreview() {
    LaimoryTheme {
        PreviewMonthPicker()
    }
}

@Preview(name = "연·월 피커 · 다크", showBackground = true, widthDp = 360)
@Composable
private fun CalendarMonthPickerDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        PreviewMonthPicker()
    }
}

@Preview(name = "연·월 피커 · 다른 해", showBackground = true, widthDp = 360)
@Composable
private fun CalendarMonthPickerOtherYearPreview() {
    LaimoryTheme {
        // 표시 월과 다른 해를 넘겨보는 중 — 선택 표시가 없어야 한다.
        PreviewMonthPicker(pickerYear = 2025)
    }
}

@Composable
private fun PreviewMonthPicker(pickerYear: Int = 2026) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(Spacing.extraLarge2),
        contentAlignment = Alignment.Center,
    ) {
        CalendarMonthPickerContent(
            pickerYear = pickerYear,
            visibleMonth = YearMonth.of(2026, 5),
            canShowPreviousYear = true,
            canShowNextYear = true,
            onPreviousYear = {},
            onNextYear = {},
            onSelectMonth = {},
        )
    }
}
