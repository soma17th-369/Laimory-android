package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeDatePickerDialog(
    initialDate: LocalDate,
    /** 이미 저장이 끝나 고를 수 없는 날짜. 아직 받지 못한 달은 비어 있다. */
    savedDates: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
    onDisplayedMonthChange: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    val tomorrowMillis = remember { LocalDate.now().plusDays(1).toUtcMillis() }
    // 피커를 다시 만들어도 보고 있던 달과 고른 날짜는 잃지 않도록 밖에서 들고 있는다.
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    // M3 피커는 `selectableDates` 를 만들 때 한 번만 받는다. 나중에 받아 온 달을 격자에 반영할
    // 길이 그것뿐이라, 집합이 바뀌면 피커를 다시 만든다. 달·선택을 되돌려주므로 사용자가 보던
    // 자리는 그대로다.
    key(savedDates) {
        val pickerState =
            rememberDatePickerState(
                // 조회가 끝나기 전에 고른 날짜가 뒤늦게 저장됨으로 판정될 수 있다. 그대로 되돌려
                // 넣으면 회색이 된 날짜가 선택된 채 남아 확인으로 확정된다 — M3 1.4 는 초기
                // 선택값을 `SelectableDates` 로 검사하지 않는다.
                initialSelectedDateMillis = selectedDate.toUtcMillis().takeIf { selectedDate !in savedDates },
                initialDisplayedMonthMillis = displayedMonth.atDay(1).toUtcMillis(),
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            if (utcTimeMillis >= tomorrowMillis) return false
                            // 저장이 끝난 날짜는 서버가 초안 생성을 409 로 거절한다. 고르게 두면
                            // 사진까지 다 고른 뒤 마지막에야 막힌다. 초안 날짜는 막지 않는다 —
                            // 서버가 이어 붙이기로 받아 준다.
                            return utcTimeMillis.toUtcLocalDate() !in savedDates
                        }
                    },
            )

        // 달을 넘길 때마다 그 달의 기록 상태를 요청한다. 열자마자 현재 값이 한 번 흘러 첫 달도 받는다.
        LaunchedEffect(pickerState) {
            snapshotFlow { pickerState.displayedMonthMillis }
                .collect { millis ->
                    val month = YearMonth.from(millis.toUtcLocalDate())
                    displayedMonth = month
                    onDisplayedMonthChange(month)
                }
        }
        LaunchedEffect(pickerState) {
            snapshotFlow { pickerState.selectedDateMillis }
                .collect { millis -> millis?.let { selectedDate = it.toUtcLocalDate() } }
        }

        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                // 고를 수 없는 날짜가 선택된 상태로 남아 있으면 확정도 막는다.
                val confirmedDate =
                    pickerState.selectedDateMillis?.toUtcLocalDate()?.takeIf { it !in savedDates }
                TextButton(
                    onClick = { confirmedDate?.let(onSelect) },
                    enabled = confirmedDate != null,
                ) {
                    Text("확인")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        ) {
            DatePicker(
                state = pickerState,
                // 기본 문구(`날짜 선택`)는 무엇을 고르는 자리인지 말해 주지 않는다.
                title = { Text(modifier = Modifier.padding(DatePickerTitlePadding), text = "초안을 만들 날짜") },
            )
        }
    }
}

/** 피커는 UTC 자정 기준 millis 로 날짜를 다룬다. 지역 시간대로 옮기면 하루가 밀린다. */
private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** M3 기본 제목과 같은 자리에 놓는다. 슬롯을 갈아끼우면 기본 여백이 함께 사라진다. */
private val DatePickerTitlePadding = PaddingValues(start = 24.dp, end = 12.dp, top = 16.dp)
