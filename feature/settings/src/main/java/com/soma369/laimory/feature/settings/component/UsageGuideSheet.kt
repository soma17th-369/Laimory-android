package com.soma369.laimory.feature.settings.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetDragHandle
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetHeader
import com.soma369.laimory.core.ui.theme.LaimoryShapes
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.settings.state.UsageGuideTimelineTarget
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.soma369.laimory.core.ui.R as CoreUiR

/**
 * `설정 › 지원 › 사용 방법`. 다시 볼 튜토리얼을 고른다.
 *
 * 설명 화면을 따로 두지 않고 실제 화면에서 다시 짚는다 — 고르면 그 화면으로 옮겨 가 코치마크가 뜬다. 시안이 없어 데이터 소스
 * 시트와 같은 머리 줄 · 테두리 줄 모양으로 그린다.
 *
 * @param timelineTarget `타임라인 편집 둘러보기` 가 열 기록. 이벤트가 있는 기록이 없으면 그 줄을 막고 이유를 적는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UsageGuideSheet(
    timelineTarget: UsageGuideTimelineTarget,
    onFull: () -> Unit,
    onHome: () -> Unit,
    onTimeline: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = LaimoryShapes.extraLarge,
        dragHandle = { LaimorySheetDragHandle() },
    ) {
        UsageGuideSheetContent(
            timelineTarget = timelineTarget,
            onFull = onFull,
            onHome = onHome,
            onTimeline = onTimeline,
            onDismiss = onDismiss,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun UsageGuideSheetContent(
    timelineTarget: UsageGuideTimelineTarget,
    onFull: () -> Unit,
    onHome: () -> Unit,
    onTimeline: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.large)
                .padding(bottom = Spacing.extraLarge2),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        LaimorySheetHeader(title = "사용 방법", onClose = onDismiss)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            UsageGuideRow(
                title = "처음부터 따라 하기",
                description = "사진을 많이 찍은 날을 골라 홈부터 타임라인까지 차례로 둘러봐요.",
                onClick = onFull,
            )
            UsageGuideRow(
                title = "홈 화면 둘러보기",
                description = "지금 보고 있는 날짜 그대로 홈의 버튼과 카드를 짚어 드려요.",
                onClick = onHome,
            )
            UsageGuideRow(
                title = "타임라인 편집 둘러보기",
                description = timelineTarget.description(),
                isEnabled = timelineTarget is UsageGuideTimelineTarget.Available || timelineTarget is UsageGuideTimelineTarget.Failed,
                onClick = onTimeline,
            )
        }
    }
}

private fun UsageGuideTimelineTarget.description(): String =
    when (this) {
        UsageGuideTimelineTarget.Loading -> "둘러볼 기록을 찾고 있어요."
        is UsageGuideTimelineTarget.Available -> "${recordDate.format(RecordDateFormat)} 기록에서 고치는 방법을 짚어 드려요."
        UsageGuideTimelineTarget.Unavailable -> "타임라인을 만들고 나면 볼 수 있어요."
        UsageGuideTimelineTarget.Failed -> "기록을 불러오지 못했어요. 눌러서 다시 시도해 주세요."
    }

@Composable
private fun UsageGuideRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(Spacing.large)
                    .alpha(if (isEnabled) 1f else DISABLED_ALPHA),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(CoreUiR.drawable.ico_default_caret_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.38f
private val RecordDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREA)

@Preview(name = "사용 방법 시트 - 라이트", showBackground = true)
@Composable
private fun UsageGuideSheetLightPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            UsageGuideSheetContent(UsageGuideTimelineTarget.Available(LocalDate.of(2026, 10, 16)), {}, {}, {}, {})
        }
    }
}

@Preview(name = "사용 방법 시트 - 다크 · 기록 없음", showBackground = true, backgroundColor = 0xFF151311)
@Composable
private fun UsageGuideSheetDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            UsageGuideSheetContent(UsageGuideTimelineTarget.Unavailable, {}, {}, {}, {})
        }
    }
}
