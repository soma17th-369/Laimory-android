package com.soma369.laimory.feature.timeline.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.core.ui.component.LaimoryTopAppBar
import com.soma369.laimory.core.ui.theme.Emotion
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.timeline.component.CalendarMonthGridView
import com.soma369.laimory.feature.timeline.component.CalendarWeekdayHeader
import com.soma369.laimory.feature.timeline.model.CalendarRecordUiModel
import com.soma369.laimory.feature.timeline.model.toCalendarMonthGrid
import com.soma369.laimory.feature.timeline.state.CalendarRecordsUiContent
import com.soma369.laimory.feature.timeline.state.CalendarUiIntent
import com.soma369.laimory.feature.timeline.state.CalendarUiState
import com.soma369.laimory.feature.timeline.viewmodel.CalendarViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarRoute(
    innerPadding: PaddingValues,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    // 탭 진입·복귀마다 서버 기록을 재동기화하고 오늘을 재계산한다. (중복 요청 방지는 ViewModel이 소유)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.sendIntent(CalendarUiIntent.Sync)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarContent(
        innerPadding = innerPadding,
        state = state,
        onIntent = viewModel::sendIntent,
        snackbarFlow = viewModel.snackbar,
    )
}

@Composable
private fun CalendarContent(
    innerPadding: PaddingValues,
    state: CalendarUiState,
    onIntent: (CalendarUiIntent) -> Unit,
    snackbarFlow: Flow<String>,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    LaunchedEffect(snackbarFlow) {
        snackbarFlow.collect(snackbarHostState::showSnackbar)
    }
    CalendarScreen(
        innerPadding = innerPadding,
        state = state,
        onIntent = onIntent,
    )
}

@Composable
private fun CalendarScreen(
    innerPadding: PaddingValues,
    state: CalendarUiState,
    onIntent: (CalendarUiIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
    ) {
        LaimoryTopAppBar(
            title = { Text(state.visibleMonth.format(VisibleMonthFormat)) },
            // 바텀바 탭 루트라 뒤로가기를 노출하지 않는다.
            onBackClick = null,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.large)
                    .padding(top = Spacing.extraSmall),
            verticalArrangement = Arrangement.spacedBy(CalendarHeaderSpacing),
        ) {
            CalendarWeekdayHeader()
            when (state.content) {
                CalendarRecordsUiContent.Loading -> CalendarLoading()
                CalendarRecordsUiContent.Empty -> CalendarEmpty()
                CalendarRecordsUiContent.LoadFailed ->
                    CalendarLoadFailed(onRetryClick = { onIntent(CalendarUiIntent.RetryLoad) })

                is CalendarRecordsUiContent.Content -> {
                    val grid = remember(state.visibleMonth) { state.visibleMonth.toCalendarMonthGrid() }
                    CalendarMonthGridView(
                        grid = grid,
                        recordsByDate = state.content.recordsByDate,
                        selectedDate = state.selectedDate,
                        today = state.today,
                        onSelectDate = { date -> onIntent(CalendarUiIntent.SelectDate(date)) },
                        onPreviousMonth = { onIntent(CalendarUiIntent.ShowPreviousMonth) },
                        onNextMonth = { onIntent(CalendarUiIntent.ShowNextMonth) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CalendarEmpty() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "아직 저장된 기록이 없어요.\n초안을 만들어 하루를 기록해보세요.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CalendarLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "기록을 불러오지 못했어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = onRetryClick,
            modifier = Modifier.padding(top = Spacing.medium),
        ) {
            Text("다시 시도")
        }
    }
}

private val VisibleMonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREA)

/** Figma 요일 헤더 ↔ 주 격자 간격. spacing 토큰에 없는 값이라 화면 로컬 상수로 둔다. */
private val CalendarHeaderSpacing = 10.dp

@Preview(name = "기본", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenDefaultPreview() = PreviewCalendarScreen(previewContent())

@Preview(name = "선택 · 다크", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenDarkPreview() = PreviewCalendarScreen(previewContent(), darkTheme = true)

@Preview(name = "빈 월", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenEmptyMonthPreview() =
    PreviewCalendarScreen(
        previewContent(),
        // 기록이 없는 월도 전체 Empty 가 아니라 정상 격자로 표시한다.
        visibleMonth = YearMonth.of(2026, 8),
    )

@Preview(name = "로딩", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenLoadingPreview() = PreviewCalendarScreen(CalendarRecordsUiContent.Loading)

@Preview(name = "전체 Empty", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenEmptyPreview() = PreviewCalendarScreen(CalendarRecordsUiContent.Empty)

@Preview(name = "조회 실패", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenLoadFailedPreview() = PreviewCalendarScreen(CalendarRecordsUiContent.LoadFailed)

@Composable
private fun PreviewCalendarScreen(
    content: CalendarRecordsUiContent,
    visibleMonth: YearMonth = YearMonth.of(2026, 5),
    darkTheme: Boolean = false,
) {
    LaimoryTheme(darkTheme = darkTheme) {
        CalendarScreen(
            innerPadding = PaddingValues(),
            state =
                CalendarUiState(
                    visibleMonth = visibleMonth,
                    selectedDate = LocalDate.of(2026, 5, 26),
                    today = LocalDate.of(2026, 5, 26),
                    content = content,
                ),
            onIntent = {},
        )
    }
}

/** 감정 5종 + 감정 미상을 모두 덮는 미리보기 데이터. */
private fun previewContent(): CalendarRecordsUiContent.Content {
    val emotions =
        listOf(
            1 to Emotion.JOY,
            2 to Emotion.CALM,
            4 to Emotion.MELLOW,
            5 to Emotion.WEARY,
            18 to Emotion.DOWN,
            26 to Emotion.JOY,
        )
    val records =
        emotions.associate { (day, emotion) ->
            val date = LocalDate.of(2026, 5, day)
            date to CalendarRecordUiModel(recordDate = date, emotion = emotion)
        } +
            LocalDate.of(2026, 5, 10).let { date ->
                date to CalendarRecordUiModel(recordDate = date, emotion = null)
            }
    return CalendarRecordsUiContent.Content(records)
}
