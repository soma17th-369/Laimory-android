package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
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
    // 카드는 좌우 끝까지 넘기며 옆 카드가 살짝 비치게 한다. 그래서 바깥 여백은 머리말 · 버튼에만 둔다.
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.large, bottom = Spacing.extraLarge2),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            Text(
                text = "어느 날로 첫 타임라인을 만들어 볼까요?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    when {
                        recommendation.dates.isEmpty() -> "최근 한 달 동안 찍은 사진을 찾지 못했어요. 만들어 볼 날을 직접 골라 주세요."
                        recommendation.dates.size == 1 -> "최근 한 달 중 사진을 가장 많이 찍은 날이에요. 그 날 사진은 직접 골라 담을 수 있어요."
                        else -> "최근 한 달 중 사진을 많이 찍은 날이에요. 옆으로 넘겨 골라 보세요."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (recommendation.dates.isNotEmpty()) {
            RecommendedDatePager(dates = recommendation.dates, onPickDate = onPickDate)
        }

        Column(
            modifier = Modifier.padding(horizontal = Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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

/**
 * 추천한 날을 한 장씩 넘겨 본다. 가운데 카드에 초점을 두고 양옆 카드가 살짝 비쳐 넘길 수 있다는 것을 알린다. 아래 점이 몇
 * 번째인지 말한다(한 장이면 없음).
 */
@Composable
private fun RecommendedDatePager(
    dates: List<RecommendedRecordDate>,
    onPickDate: (LocalDate) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { dates.size })
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = PagerSidePeek),
            pageSpacing = Spacing.medium,
            verticalAlignment = Alignment.Top,
        ) { page ->
            val date = dates[page]
            RecommendedDateCard(date = date, onClick = { onPickDate(date.date) })
        }
        if (dates.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                // 화면 읽기 도구는 카드마다 날짜를 읽으므로 점은 건너뛴다.
                modifier = Modifier.clearAndSetSemantics { },
            ) {
                repeat(dates.size) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(PageDotSize)
                                .background(
                                    color =
                                        if (index == pagerState.currentPage) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                    shape = CircleShape,
                                ),
                    )
                }
            }
        }
    }
}

/**
 * 추천한 날 한 장. 대표 사진(그 날 마지막으로 찍은 사진)을 크게 두고 날짜를 얹는다. 아래 줄에 다른 사진 넉 장까지 두고, 더
 * 있으면 마지막 칸에 남은 장수를 적는다.
 */
@Composable
private fun RecommendedDateCard(
    date: RecommendedRecordDate,
    onClick: () -> Unit,
) {
    val dateLabel = date.date.format(RecommendedDateFormat)
    val countLabel = recommendedCountLabel(date)
    val strip = date.photoUris.drop(1)
    val hiddenCount = date.capturedPhotoCount - date.photoUris.size
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
        Column(
            modifier = Modifier.padding(Spacing.small).clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(COVER_ASPECT_RATIO)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = date.coverPhotoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(Spacing.small)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(DateChipCornerRadius))
                            .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
                )
            }
            if (strip.isNotEmpty()) {
                // 칸 크기를 사진 수와 상관없이 같게 둔다. 두세 장뿐인 날도 칸이 커지지 않는다.
                Row(horizontalArrangement = Arrangement.spacedBy(StripSpacing)) {
                    repeat(STRIP_SLOTS) { index ->
                        val uri = strip.getOrNull(index)
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                            if (uri != null) {
                                StripPhoto(uri = uri, hiddenCount = hiddenCount.takeIf { index == strip.lastIndex && it > 0 })
                            }
                        }
                    }
                }
            }
            Text(
                text = countLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.extraSmall),
            )
        }
    }
}

/** 아래 줄 사진 한 칸. [hiddenCount] 가 있으면 어둡게 덮고 `+n` 을 적는다. */
@Composable
private fun StripPhoto(
    uri: String,
    hiddenCount: Int?,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (hiddenCount != null) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = HIDDEN_SCRIM_ALPHA)))
            Text(text = "+$hiddenCount", style = MaterialTheme.typography.titleSmall, color = Color.White)
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
private val PagerSidePeek = 40.dp
private val PageDotSize = 6.dp
private val StripSpacing = 4.dp
private val DateChipCornerRadius = 8.dp
private const val STRIP_SLOTS = 4
private const val COVER_ASPECT_RATIO = 4f / 3f
private const val HIDDEN_SCRIM_ALPHA = 0.5f

private val PreviewDates =
    listOf(
        RecommendedRecordDate(
            LocalDate.of(2026, 10, 18),
            capturedPhotoCount = 24,
            calendarEventCount = 2,
            photoUris = listOf("", "", "", "", ""),
        ),
        RecommendedRecordDate(
            LocalDate.of(2026, 10, 11),
            capturedPhotoCount = 15,
            calendarEventCount = 0,
            photoUris = listOf("", "", "", "", ""),
        ),
        RecommendedRecordDate(
            LocalDate.of(2026, 10, 3),
            capturedPhotoCount = 9,
            calendarEventCount = 1,
            photoUris = listOf("", "", "", "", ""),
        ),
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
