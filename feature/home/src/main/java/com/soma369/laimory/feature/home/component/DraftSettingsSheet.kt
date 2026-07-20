package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomeTimeField
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DraftSettingsSheet(
    state: HomeUiState,
    onIntent: (HomeUiIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isValid = state.recordDateWindow(ZoneId.systemDefault()) != null
    ModalBottomSheet(
        onDismissRequest = { onIntent(HomeUiIntent.DismissDraftSheet) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.extraLarge, vertical = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text("초안 범위 설정", style = MaterialTheme.typography.titleLarge)
                Text(
                    "선택한 날짜의 시작 시각부터 당일 또는 익일 종료 시각까지 모은 데이터를 사용해요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingRow(
                label = "기준 날짜",
                value = state.selectedDate.format(DATE_FORMAT),
                onClick = { onIntent(HomeUiIntent.ShowDatePicker) },
            )
            SettingRow(
                label = "시작 시각",
                value = state.startTime.format(TIME_FORMAT),
                onClick = { onIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.START)) },
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text(
                    "종료일",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    FilterChip(
                        selected = state.endDay == DraftEndDay.SAME_DAY,
                        onClick = { onIntent(HomeUiIntent.SelectEndDay(DraftEndDay.SAME_DAY)) },
                        label = { Text("당일") },
                    )
                    FilterChip(
                        selected = state.endDay == DraftEndDay.NEXT_DAY,
                        onClick = { onIntent(HomeUiIntent.SelectEndDay(DraftEndDay.NEXT_DAY)) },
                        label = { Text("익일") },
                    )
                }
            }
            SettingRow(
                label = "종료 시각",
                value = state.endTime.format(TIME_FORMAT),
                onClick = { onIntent(HomeUiIntent.ShowTimePicker(HomeTimeField.END)) },
            )

            if (!isValid) {
                Text(
                    "종료 시각은 시작 시각보다 뒤로 설정해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(
                text = "선택 범위에 모인 데이터 ${state.summary.totalItemCount}건",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { onIntent(HomeUiIntent.CreateDraft) },
                enabled =
                    isValid &&
                        state.summary.totalItemCount > 0 &&
                        state.draftStatus != DraftCreationStatus.SUBMITTING &&
                        state.draftStatus != DraftCreationStatus.SUBMITTED,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.draftStatus == DraftCreationStatus.SUBMITTING) {
                        "초안 생성 중…"
                    } else if (state.draftStatus == DraftCreationStatus.SUBMITTED) {
                        "초안 생성 요청 완료"
                    } else {
                        "이 범위로 초안 만들기"
                    },
                )
            }
            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
