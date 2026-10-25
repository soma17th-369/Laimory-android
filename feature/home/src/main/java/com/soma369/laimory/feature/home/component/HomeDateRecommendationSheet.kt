package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soma369.laimory.core.domain.model.tutorial.RecommendedRecordDate
import com.soma369.laimory.core.ui.component.sheet.LaimorySheetDragHandle
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.home.state.HomeDateRecommendation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 홈 튜토리얼의 첫 장 — 최근 한 달 중 사진을 많이 찍은 날을 추천해 첫 타임라인을 만들어 볼 날을 고르게 한다.
 *
 * 시안이 없어 기존 시트 모양(손잡이 · 머리 제목 · 테두리 카드 · 전폭 버튼)으로 그린다. 추천은 날짜까지만 하고, 사진은 그
 * 날 홈 사진 카드에서 사용자가 고른다 — 본문에서 그렇게 알린다.
 *
 * 추천이 없으면(권한 없음 · 찍은 사진 없음) 그 사실과 `다른 날 직접 고르기` · `건너뛰기` 만 남는다. 바깥 누름 · 쓸어내리기 ·
 * 뒤로가기는 건너뛰기와 같다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeDateRecommendationSheet(
    recommendation: HomeDateRecommendation,
    onPickDate: (LocalDate) -> Unit,
    onPickOtherDate: () -> Unit,
    onSkip: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = { LaimorySheetDragHandle(topPadding = Spacing.medium, bottomPadding = Spacing.small) },
    ) {
        HomeDateRecommendationContent(
            recommendation = recommendation,
            onPickDate = onPickDate,
            onPickOtherDate = onPickOtherDate,
            onSkip = onSkip,
        )
    }
}

@Composable
private fun HomeDateRecommendationContent(
    recommendation: HomeDateRecommendation,
    onPickDate: (LocalDate) -> Unit,
    onPickOtherDate: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Spacing.large, end = Spacing.large, top = Spacing.large, bottom = Spacing.extraLarge2),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
            Text(
                text = "어느 날로 첫 타임라인을 만들어 볼까요?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    if (recommendation.dates.isEmpty()) {
                        "최근 한 달 동안 찍은 사진을 찾지 못했어요. 만들어 볼 날을 직접 골라 주세요."
                    } else {
                        "최근 한 달 중 사진을 많이 찍은 날이에요. 날을 고르면 그 날 사진은 직접 골라 담을 수 있어요."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (recommendation.dates.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                recommendation.dates.forEach { date ->
                    RecommendedDateCard(date = date, onClick = { onPickDate(date.date) })
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedButton(
                onClick = onPickOtherDate,
                modifier = Modifier.fillMaxWidth().height(ButtonHeight),
                shape = RoundedCornerShape(ButtonCornerRadius),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(text = "다른 날 직접 고르기", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onSkip) {
                Text(
                    text = "건너뛰기",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecommendedDateCard(
    date: RecommendedRecordDate,
    onClick: () -> Unit,
) {
    val dateLabel = date.date.format(RecommendedDateFormat)
    val countLabel = recommendedCountLabel(date)
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = "$dateLabel, $countLabel" },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = date.coverPhotoUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(ThumbnailSize)
                        .clip(MaterialTheme.shapes.medium)
                        .clearAndSetSemantics { },
            )
            Column(
                modifier = Modifier.weight(1f).clearAndSetSemantics { },
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            ) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** `찍은 사진 12장 · 일정 2개`. 일정이 없으면 사진만 적는다. */
internal fun recommendedCountLabel(date: RecommendedRecordDate): String =
    buildString {
        append("찍은 사진 ${date.capturedPhotoCount}장")
        if (date.calendarEventCount > 0) append(" · 일정 ${date.calendarEventCount}개")
    }

private val RecommendedDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREA)
private val SheetCornerRadius = 28.dp
private val ButtonHeight = 52.dp
private val ButtonCornerRadius = 12.dp
private val ThumbnailSize = 56.dp

private val PreviewDates =
    listOf(
        RecommendedRecordDate(LocalDate.of(2026, 10, 18), capturedPhotoCount = 24, calendarEventCount = 2, coverPhotoUri = ""),
        RecommendedRecordDate(LocalDate.of(2026, 10, 11), capturedPhotoCount = 15, calendarEventCount = 0, coverPhotoUri = ""),
        RecommendedRecordDate(LocalDate.of(2026, 10, 3), capturedPhotoCount = 9, calendarEventCount = 1, coverPhotoUri = ""),
    )

@Preview(name = "추천 날짜 시트 - 라이트", showBackground = true)
@Composable
private fun HomeDateRecommendationLightPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            HomeDateRecommendationContent(HomeDateRecommendation(PreviewDates), {}, {}, {})
        }
    }
}

@Preview(name = "추천 날짜 시트 - 다크", showBackground = true, backgroundColor = 0xFF151311)
@Composable
private fun HomeDateRecommendationDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            HomeDateRecommendationContent(HomeDateRecommendation(PreviewDates), {}, {}, {})
        }
    }
}

@Preview(name = "추천 날짜 시트 - 추천 없음", showBackground = true)
@Composable
private fun HomeDateRecommendationEmptyPreview() {
    LaimoryTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            HomeDateRecommendationContent(HomeDateRecommendation(emptyList()), {}, {}, {})
        }
    }
}
