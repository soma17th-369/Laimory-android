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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    val tomorrowMillis =
        remember {
            LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
    // 판정 객체는 한 번만 만들어지므로 집합을 직접 붙잡으면 나중에 받은 달이 반영되지 않는다.
    // 최신 값을 가리키는 상태를 읽어, 달을 받아 올 때마다 격자가 다시 판정되게 한다.
    val latestSavedDates = rememberUpdatedState(savedDates)
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates =
                remember {
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            if (utcTimeMillis >= tomorrowMillis) return false
                            // 저장이 끝난 날짜는 서버가 초안 생성을 409 로 거절한다. 고르게 두면
                            // 사진까지 다 고른 뒤 마지막에야 막힌다. 초안 날짜는 막지 않는다 —
                            // 서버가 이어 붙이기로 받아 준다.
                            return utcTimeMillis.toUtcLocalDate() !in latestSavedDates.value
                        }
                    }
                },
        )

    // 달을 넘길 때마다 그 달의 기록 상태를 요청한다. 열자마자 현재 값이 한 번 흘러 첫 달도 받는다.
    LaunchedEffect(pickerState) {
        snapshotFlow { pickerState.displayedMonthMillis }
            .collect { millis -> onDisplayedMonthChange(YearMonth.from(millis.toUtcLocalDate())) }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis -> onSelect(millis.toUtcLocalDate()) }
                },
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

/** 피커는 UTC 자정 기준 millis 로 날짜를 다룬다. 지역 시간대로 옮기면 하루가 밀린다. */
private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** M3 기본 제목과 같은 자리에 놓는다. 슬롯을 갈아끼우면 기본 여백이 함께 사라진다. */
private val DatePickerTitlePadding = PaddingValues(start = 24.dp, end = 12.dp, top = 16.dp)
