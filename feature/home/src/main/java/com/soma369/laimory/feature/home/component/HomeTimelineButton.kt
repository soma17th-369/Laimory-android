package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimoryColors
import com.soma369.laimory.feature.home.state.DraftCreationStatus
import java.time.LocalDate
import com.soma369.laimory.core.ui.R as UiR

/**
 * 홈 시그니처 CTA(Figma `timeline-button` 2516:12528).
 *
 * 세 변형이 같은 자리에 선다 — 만들기·제작중·확인하기. 상태마다 버튼을 따로 두지 않는 이유는
 * 누를 자리가 옮겨 다니면 매번 찾아야 하기 때문이다.
 *
 * 실패(`FAILED`)는 `만들기` 변형 그대로다. 시안에 `다시 시도` 전용 변형이 없고, 실패 문구는
 * 지금처럼 스낵바가 알린다.
 */
@Composable
internal fun HomeTimelineButton(
    status: DraftCreationStatus,
    selectedDate: LocalDate,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = status.actionTitle()
    val subtitle = momentSubtitle(selectedDate, today)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(status.gradient())
                .clickable(onClick = onClick)
                // 높이를 고정하지 않는다(시안 72 = 위아래 12 + 원 48). 고정하면 큰 글꼴에서 부제가 잘린다.
                .padding(horizontal = Spacing.large, vertical = Spacing.medium)
                // 제목·부제·화살표를 따로 읽으면 한 버튼을 세 번 지난다.
                .clearAndSetSemantics { contentDescription = "$title. $subtitle" },
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ico_home_cta_clock_countdown),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Icon(
            painter = painterResource(UiR.drawable.ico_default_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private fun DraftCreationStatus.actionTitle(): String =
    when (this) {
        DraftCreationStatus.IDLE, DraftCreationStatus.FAILED -> "타임라인 만들기"
        DraftCreationStatus.PROCESSING, DraftCreationStatus.LONG_RUNNING -> "타임라인 제작중"
        DraftCreationStatus.SUCCESS -> "타임라인 확인하기"
    }

/** 좌→우 그라데이션. 진한 컨테이너색에서 밝은 본색으로 흐른다. 양 끝 10% 는 단색이다(시안 10.7%→89.6%). */
@Composable
private fun DraftCreationStatus.gradient(): Brush {
    val (start, end) =
        when (this) {
            DraftCreationStatus.IDLE, DraftCreationStatus.FAILED ->
                MaterialTheme.colorScheme.onPrimaryContainer to MaterialTheme.colorScheme.primary

            DraftCreationStatus.PROCESSING, DraftCreationStatus.LONG_RUNNING ->
                MaterialTheme.laimoryColors.onWarningContainer to MaterialTheme.laimoryColors.warning

            DraftCreationStatus.SUCCESS ->
                MaterialTheme.colorScheme.onSecondaryContainer to MaterialTheme.colorScheme.secondary
        }
    return Brush.horizontalGradient(GRADIENT_START to start, GRADIENT_END to end)
}

/** 어느 날의 순간인지. 오늘·어제만 말로 부르고 나머지는 날짜를 적는다. */
private fun momentSubtitle(
    date: LocalDate,
    today: LocalDate,
): String {
    val subject =
        when (date) {
            today -> "오늘"
            today.minusDays(1) -> "어제"
            else -> "${date.monthValue}월 ${date.dayOfMonth}일"
        }
    return "${subject}의 순간을 하나로"
}

private const val GRADIENT_START = 0.107f
private const val GRADIENT_END = 0.896f
