package com.soma369.laimory.feature.home.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.loading.DraftLoadingStage
import com.soma369.laimory.feature.home.loading.DraftLoadingStageState
import com.soma369.laimory.core.ui.R as UiR

/** 진행 카드 한 줄에 필요한 표시값. */
internal data class DraftLoadingProgressRow(
    val stage: DraftLoadingStage,
    val label: String,
    val state: DraftLoadingStageState,
    /** 완료 시 보여줄 문구. 예: `13장 완료`. */
    val doneLabel: String,
    /** 진행 중일 때 보여줄 문구. 예: `분석 중...`. */
    val progressLabel: String,
)

/**
 * 네 단계를 한 카드에 세로로 쌓는다.
 *
 * 앞 세 줄은 대기 연출이고 마지막 줄만 서버 완료로 끝난다. 그래서 완료 줄만 강조색 문구를 쓰고,
 * 진행 중·대기 줄은 보조색으로 두어 아직 확정된 정보가 아님을 시각적으로도 구분한다.
 */
@Composable
internal fun DraftLoadingProgressCard(
    rows: List<DraftLoadingProgressRow>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardCornerRadius))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(CardCornerRadius)),
    ) {
        rows.forEachIndexed { index, row ->
            ProgressRow(row)
            if (index != rows.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ProgressRow(row: DraftLoadingProgressRow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(RowPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StageIcon(row.state)
        Text(
            text = row.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color =
                if (row.state == DraftLoadingStageState.DONE) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        when (row.state) {
            DraftLoadingStageState.DONE ->
                Text(
                    text = row.doneLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )

            DraftLoadingStageState.IN_PROGRESS ->
                Text(
                    text = row.progressLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

            DraftLoadingStageState.PENDING ->
                Text(
                    text = "대기 중",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
    }
}

@Composable
private fun StageIcon(state: DraftLoadingStageState) {
    when (state) {
        DraftLoadingStageState.DONE ->
            Box(
                modifier = Modifier.size(IconSize).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ico_default_check),
                    contentDescription = null,
                    modifier = Modifier.size(CheckSize),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }

        DraftLoadingStageState.IN_PROGRESS -> InProgressIcon()

        DraftLoadingStageState.PENDING ->
            Box(
                modifier =
                    Modifier
                        .size(IconSize)
                        .border(RingWidth, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
    }
}

/**
 * 진행 중 표시.
 *
 * 가운데 점 뒤로 원이 퍼지며 옅어지는 것을 반복해, 멈춘 것이 아니라 지금 돌아가고 있음을 보인다.
 * 테두리는 그대로 두고 파동만 더해 Figma의 모양은 유지한다.
 */
@Composable
private fun InProgressIcon() {
    val transition = rememberInfiniteTransition(label = "stageProgress")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = PULSE_DURATION_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulse",
    )
    Box(
        modifier =
            Modifier
                .size(IconSize)
                .border(RingWidth, MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // 퍼지는 원은 점에서 시작해 테두리까지 자라며 사라진다.
        Box(
            modifier =
                Modifier
                    .size(lerp(DotSize, IconSize, pulse))
                    .alpha((1f - pulse) * PULSE_MAX_ALPHA)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Box(modifier = Modifier.size(DotSize).background(MaterialTheme.colorScheme.primary, CircleShape))
    }
}

private const val PULSE_DURATION_MILLIS = 1_400
private const val PULSE_MAX_ALPHA = 0.4f
private val CardCornerRadius = 20.dp
private val RowPadding = 14.dp
private val IconSize = 20.dp
private val CheckSize = 10.dp
private val RingWidth = 1.5.dp
private val DotSize = 6.dp
