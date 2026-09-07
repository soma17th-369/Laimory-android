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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import com.soma369.laimory.feature.home.state.HomeSourceSummary
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.state.isInputLocked
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Figma DateHeaderCard(760:2008)를 홈의 실제 요약 데이터와 생성 상태에 연결한 카드. */
@Composable
internal fun DateHeaderCard(
    state: HomeUiState,
    onClick: () -> Unit,
    onActionClick: () -> Unit,
    onTimeRangeClick: () -> Unit,
) {
    val today = rememberToday()
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                    Text(
                        text = state.selectedDate.format(CARD_DATE_FORMAT),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TimeRangeChip(state = state, onClick = onTimeRangeClick)
            }

            Text(
                text = momentTitle(state.selectedDate, today),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            )

            // 사진은 초안 만들기 흐름 안에서 고른다. 여기서도 열 수 있게 두면 어느 자리에서
            // 고른 것이 실리는지 알 수 없어, 미리보기는 표시만 맡는다.
            PhotoPreviewRow(
                previewUris = state.summary.photoPreviewUris,
                photoCount = state.summary.photoCount,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        "사진 ${state.summary.photoCount} · 일정 ${state.summary.calendarCount} · " +
                            "걸음 ${formatNumber(state.summary.stepCount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onActionClick)
                            .padding(Spacing.extraSmall),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = draftActionLabel(state.draftStatus),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ico_default_caret_right),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * 기록 범위 칩. 값을 보여 주는 자리가 그대로 고치는 자리다.
 *
 * 범위를 묻는 중간 시트를 없앤 대신 여기서 공용 타임 피커를 연다 — 대부분은 기본값을 그대로
 * 쓰고 넘어가므로, 매번 확인시키는 단계보다 필요할 때 누르는 편이 낫다.
 */
@Composable
private fun TimeRangeChip(
    state: HomeUiState,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = !state.draftStatus.isInputLocked,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = Spacing.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ico_timeline_clock),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = state.timeRangeLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhotoPreviewRow(
    previewUris: List<String>,
    photoCount: Int,
) {
    if (previewUris.isEmpty()) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ico_timeline_photo),
                    contentDescription = "아직 모은 사진이 없어요",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        previewUris.forEachIndexed { index, uri ->
            PhotoPreview(
                uri = uri,
                contentDescription = "모은 사진 미리보기 ${index + 1}",
            )
        }
        val remaining = photoCount - previewUris.size
        if (remaining > 0) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+$remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoPreview(
    uri: String,
    contentDescription: String,
) {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
        )
    }
}

private fun momentTitle(
    date: LocalDate,
    today: LocalDate,
): String {
    val subject =
        when (date) {
            today -> "오늘의 순간들을"
            today.minusDays(1) -> "어제의 순간들을"
            else -> "${date.monthValue}월 ${date.dayOfMonth}일의 순간들을"
        }
    return "$subject\n모아봤어요."
}

private fun draftActionLabel(status: DraftCreationStatus): String =
    when (status) {
        DraftCreationStatus.IDLE -> "초안 만들기"
        DraftCreationStatus.PROCESSING -> "초안 생성 중"
        DraftCreationStatus.LONG_RUNNING -> "오래 걸리고 있어요"
        DraftCreationStatus.SUCCESS -> "초안 보기"
        DraftCreationStatus.FAILED -> "다시 시도"
    }

private fun formatNumber(value: Long): String = String.format(Locale.KOREA, "%,d", value)

private val CARD_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREA)

@Preview(name = "초안 처리 중", showBackground = true)
@Composable
private fun DateHeaderCardProcessingPreview() = DraftStatusPreview(DraftCreationStatus.PROCESSING)

@Preview(name = "초안 장기 처리", showBackground = true)
@Composable
private fun DateHeaderCardLongRunningPreview() = DraftStatusPreview(DraftCreationStatus.LONG_RUNNING)

@Preview(name = "초안 성공", showBackground = true)
@Composable
private fun DateHeaderCardSuccessPreview() = DraftStatusPreview(DraftCreationStatus.SUCCESS)

@Preview(name = "초안 실패", showBackground = true)
@Composable
private fun DateHeaderCardFailedPreview() = DraftStatusPreview(DraftCreationStatus.FAILED)

@Composable
private fun DraftStatusPreview(status: DraftCreationStatus) {
    LaimoryTheme {
        DateHeaderCard(
            state =
                HomeUiState(
                    selectedDate = LocalDate.of(2026, 7, 22),
                    summary = HomeSourceSummary(photoCount = 3, calendarCount = 2, stepCount = 4_821),
                    draftStatus = status,
                ),
            onClick = {},
            onActionClick = {},
            onTimeRangeClick = {},
        )
    }
}
