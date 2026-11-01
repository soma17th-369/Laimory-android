package com.soma369.laimory.core.ui.reminder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.soma369.laimory.core.ui.component.timepicker.LaimoryTimePickerSheet
import com.soma369.laimory.core.ui.component.timepicker.LaimoryTimePickerValue
import com.soma369.laimory.core.ui.component.timepicker.TimePickerField
import com.soma369.laimory.core.ui.component.timepicker.TimePickerMinuteStep
import java.time.LocalDate
import java.time.LocalTime

/**
 * 일일 리마인더 시간대 시트. 설정과 온보딩이 같은 시트를 연다.
 *
 * 정시만 고른다([TimePickerMinuteStep.HOUR]). 리마인더는 정확 알람 권한 없이 예약해 정각이 아니라 그 무렵에 오므로,
 * 분을 고르게 해도 지킬 수 없다. 그 사실은 시트 안내 한 줄로만 알린다 — 줄마다 붙이면 시끄럽다.
 */
@Composable
fun DailyReminderTimeSheet(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 줄이 하나라 처음부터 펼쳐 둔다. 접는 것은 화면의 일이 아니라 시트 안의 표시라 여기서 든다.
    var expandedFieldId by remember { mutableStateOf<String?>(FIELD_ID) }
    LaimoryTimePickerSheet(
        fields =
            listOf(
                TimePickerField(
                    id = FIELD_ID,
                    label = DAILY_REMINDER_TIME_LABEL,
                    value = LaimoryTimePickerValue(date = PLACEHOLDER_DATE, time = time),
                    minuteStep = TimePickerMinuteStep.HOUR,
                ),
            ),
        expandedFieldId = expandedFieldId,
        onExpandedFieldChange = { id -> expandedFieldId = id },
        onValueChange = { _, value, _ -> onTimeChange(value.time) },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        title = "일일 리마인더",
        supportingText = "휴대폰 상태에 따라 조금 늦게 올 수 있어요.",
    )
}

/** 리마인더 시간대 줄의 이름. 설정과 온보딩, 시트가 같은 말로 부른다. */
const val DAILY_REMINDER_TIME_LABEL = "알림 시간대"

private const val FIELD_ID = "daily_reminder_time"

/** 피커 값이 요구하는 날짜 자리. 날짜 열이 없어 화면에 나오지 않고 바뀌지도 않는다. */
private val PLACEHOLDER_DATE: LocalDate = LocalDate.of(2000, 1, 1)
