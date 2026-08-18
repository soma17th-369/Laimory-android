package com.soma369.laimory.core.ui.component.timepicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetDragHandle
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetHeader
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.tabularFigures
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 타임 피커 시트가 다루는 시각 항목 한 개.
 *
 * [dates]는 날짜 롤러 선택지다. 비어 있으면 날짜를 아예 다루지 않고(표시도 하지 않고), 1개면
 * 날짜를 값 표시에만 쓰며 롤러 열은 감춘다. 2개 이상일 때만 날짜 열을 함께 굴린다.
 */
@Immutable
data class TimePickerField(
    val id: String,
    val label: String,
    val value: LaimoryTimePickerValue,
    val dates: List<TimePickerDateOption> = emptyList(),
    val minuteStep: TimePickerMinuteStep = TimePickerMinuteStep.FIVE,
)

/**
 * Laimory 공통 타임 피커 시트.
 *
 * 시작·종료처럼 서로 이어지는 여러 시각을 한 시트에서 다룬다. 각 항목은 한 줄로 접혀 있고 줄을
 * 누르면 그 아래에 롤러가 펼쳐지며, 펼침은 한 번에 하나만 유지한다.
 *
 * 상태를 스스로 갖지 않는다 — 값·펼침 모두 호출부가 소유하므로, 되돌리기(취소)를 원하면 화면이
 * 임시 값을 들고 있다가 [onConfirm]에서 확정하면 된다. 시트를 닫는 것도 [onDismiss]·[onConfirm]을
 * 받은 화면의 몫이다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaimoryTimePickerSheet(
    fields: List<TimePickerField>,
    expandedFieldId: String?,
    onExpandedFieldChange: (String?) -> Unit,
    onValueChange: (String, LaimoryTimePickerValue, TimePickerColumn) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "시간 설정",
    confirmLabel: String = "확인",
    confirmEnabled: Boolean = true,
) {
    // 시트 상태는 밖으로 내보내지 않는다 — 실험 API를 호출부까지 번지게 하지 않기 위해서다.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = { LaimorySheetDragHandle() },
    ) {
        TimePickerSheetContent(
            fields = fields,
            expandedFieldId = expandedFieldId,
            onExpandedFieldChange = onExpandedFieldChange,
            onValueChange = onValueChange,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            title = title,
            confirmLabel = confirmLabel,
            confirmEnabled = confirmEnabled,
        )
    }
}

@Composable
private fun TimePickerSheetContent(
    fields: List<TimePickerField>,
    expandedFieldId: String?,
    onExpandedFieldChange: (String?) -> Unit,
    onValueChange: (String, LaimoryTimePickerValue, TimePickerColumn) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
) {
    // 롤러를 펼치면 세로 여유가 줄어드는 만큼 줄 간격도 함께 좁힌다(Figma Collapsed/Expanded).
    // 롤러가 나타나고 사라지는 동안 여백이 한 번에 튀면 같이 끊겨 보이므로 함께 이어서 움직인다.
    val isAnyExpanded = fields.any { it.id == expandedFieldId }
    val sectionSpacing by animateDpAsState(
        targetValue = if (isAnyExpanded) Spacing.extraLarge else Spacing.extraLarge2,
        label = "sectionSpacing",
    )
    val headerPadding by animateDpAsState(
        targetValue = if (isAnyExpanded) Spacing.extraSmall else Spacing.small,
        label = "headerPadding",
    )
    val rowPadding by animateDpAsState(
        targetValue = if (isAnyExpanded) Spacing.medium else Spacing.large,
        label = "rowPadding",
    )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(bottom = Spacing.extraLarge2),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
    ) {
        LaimorySheetHeader(
            title = title,
            onClose = onDismiss,
            verticalPadding = headerPadding,
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            fields.forEachIndexed { index, field ->
                val isExpanded = field.id == expandedFieldId
                TimeRow(
                    field = field,
                    isExpanded = isExpanded,
                    verticalPadding = rowPadding,
                    onClick = { onExpandedFieldChange(if (isExpanded) null else field.id) },
                )
                AnimatedVisibility(visible = isExpanded) {
                    TimePickerRoller(
                        value = field.value,
                        dates = field.dates,
                        minuteStep = field.minuteStep,
                        onValueChange = { value, column -> onValueChange(field.id, value, column) },
                    )
                }
                if (index != fields.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth().height(ConfirmButtonHeight),
            onClick = onConfirm,
            enabled = confirmEnabled,
            shape = RoundedCornerShape(ConfirmButtonCornerRadius),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        ) {
            Text(
                text = confirmLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun TimeRow(
    field: TimePickerField,
    isExpanded: Boolean,
    verticalPadding: Dp,
    onClick: () -> Unit,
) {
    val valueLabel = field.value.displayLabel(field.dates)
    val valueColor = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = verticalPadding)
                .semantics {
                    contentDescription =
                        "${field.label} $valueLabel. ${if (isExpanded) "시각 선택 닫기" else "시각 선택 열기"}"
                },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = field.label,
            modifier = Modifier.clearAndSetSemantics {},
            // 펼쳐도 글자 크기는 그대로 두고 색만 바꾼다. 줄여 놓으면 지금 고르고 있는 줄이 오히려
            // 작아져 어색하다.
            style = MaterialTheme.typography.titleMedium,
            color =
                if (isExpanded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Row(
            modifier = Modifier.clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.titleMedium.tabularFigures(),
                color = valueColor,
            )
            Icon(
                painter =
                    painterResource(
                        if (isExpanded) R.drawable.ico_default_chevron_up else R.drawable.ico_default_chevron_right,
                    ),
                contentDescription = null,
                modifier = Modifier.size(RowChevronSize),
                tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 줄에 표시할 값. 날짜 선택지가 아예 없으면 시각만, 있으면 `(MM.dd) HH:mm` 형태로 보여준다. */
private fun LaimoryTimePickerValue.displayLabel(dates: List<TimePickerDateOption>): String {
    val formattedTime = time.format(RowTimeFormatter)
    return if (dates.isEmpty()) formattedTime else "(${date.format(RowDateFormatter)}) $formattedTime"
}

private val RowDateFormatter = DateTimeFormatter.ofPattern("MM.dd")
private val RowTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private val SheetCornerRadius = 24.dp
private val RowChevronSize = 16.dp
private val ConfirmButtonHeight = 52.dp
private val ConfirmButtonCornerRadius = 16.dp

@Preview(name = "TimePickerSheet / Collapsed", showBackground = true, widthDp = 375, heightDp = 560)
@Composable
private fun LaimoryTimePickerSheetCollapsedPreview() {
    LaimoryTheme {
        Column {
            LaimorySheetDragHandle()
            TimePickerSheetContent(
                fields = previewFields(),
                expandedFieldId = null,
                onExpandedFieldChange = {},
                onValueChange = { _, _, _ -> },
                onConfirm = {},
                onDismiss = {},
                title = "시간 설정",
                confirmLabel = "확인",
                confirmEnabled = true,
            )
        }
    }
}

@Preview(name = "TimePickerSheet / Expanded", showBackground = true, widthDp = 375, heightDp = 560)
@Composable
private fun LaimoryTimePickerSheetExpandedPreview() {
    LaimoryTheme {
        Column {
            LaimorySheetDragHandle()
            TimePickerSheetContent(
                fields = previewFields(),
                expandedFieldId = "start",
                onExpandedFieldChange = {},
                onValueChange = { _, _, _ -> },
                onConfirm = {},
                onDismiss = {},
                title = "시간 설정",
                confirmLabel = "확인",
                confirmEnabled = true,
            )
        }
    }
}

@Preview(name = "TimePickerSheet / 날짜 열 없음", showBackground = true, widthDp = 375, heightDp = 560)
@Composable
private fun LaimoryTimePickerSheetWithoutDateColumnPreview() {
    LaimoryTheme {
        Column {
            LaimorySheetDragHandle()
            TimePickerSheetContent(
                fields =
                    listOf(
                        TimePickerField(
                            id = "bed",
                            label = "잠든 시각",
                            value = LaimoryTimePickerValue(previewDate, LocalTime.of(23, 40)),
                        ),
                        TimePickerField(
                            id = "wake",
                            label = "일어난 시각",
                            value = LaimoryTimePickerValue(previewDate.plusDays(1), LocalTime.of(7, 20)),
                        ),
                    ),
                expandedFieldId = "bed",
                onExpandedFieldChange = {},
                onValueChange = { _, _, _ -> },
                onConfirm = {},
                onDismiss = {},
                title = "수면 시간",
                confirmLabel = "확인",
                confirmEnabled = true,
            )
        }
    }
}

private val previewDate = LocalDate.of(2026, 8, 1)

private fun previewFields() =
    listOf(
        TimePickerField(
            id = "start",
            label = "시작 시각",
            value = LaimoryTimePickerValue(previewDate, LocalTime.of(16, 10)),
            dates = listOf(TimePickerDateOption(previewDate, "당일")),
            minuteStep = TimePickerMinuteStep.ONE,
        ),
        TimePickerField(
            id = "end",
            label = "종료 시각",
            value = LaimoryTimePickerValue(previewDate, LocalTime.of(20, 10)),
            dates =
                listOf(
                    TimePickerDateOption(previewDate, "당일"),
                    TimePickerDateOption(previewDate.plusDays(1), "익일"),
                ),
            minuteStep = TimePickerMinuteStep.ONE,
        ),
    )
