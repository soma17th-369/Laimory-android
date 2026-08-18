package com.soma369.laimory.feature.timeline.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.soma369.laimory.feature.timeline.component.CalendarMonthPager
import com.soma369.laimory.feature.timeline.component.CalendarMonthPickerDialog
import com.soma369.laimory.feature.timeline.component.CalendarWeekdayHeader
import com.soma369.laimory.feature.timeline.model.CALENDAR_YEAR_RANGE
import com.soma369.laimory.feature.timeline.model.CalendarRecordUiModel
import com.soma369.laimory.feature.timeline.state.CalendarMonthPickerState
import com.soma369.laimory.feature.timeline.state.CalendarUiIntent
import com.soma369.laimory.feature.timeline.state.CalendarUiState
import com.soma369.laimory.feature.timeline.state.MonthlyRecordsUiContent
import com.soma369.laimory.feature.timeline.viewmodel.CalendarViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.soma369.laimory.core.ui.R as UiR

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
            title = {
                VisibleMonthTitle(
                    visibleMonth = state.visibleMonth,
                    onClick = { onIntent(CalendarUiIntent.OpenMonthPicker) },
                )
            },
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
            // 로딩·실패도 페이지 안에서 표현하므로 pager 는 최초 진입부터 계속 떠 있다.
            CalendarMonthPager(
                visibleMonth = state.visibleMonth,
                months = state.months,
                selectedDate = state.selectedDate,
                today = state.today,
                onSelectDate = { date -> onIntent(CalendarUiIntent.SelectDate(date)) },
                onVisibleMonthChange = { month -> onIntent(CalendarUiIntent.ShowMonth(month)) },
                onRetryMonth = { month -> onIntent(CalendarUiIntent.RetryMonth(month)) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    state.monthPicker?.let { picker ->
        CalendarMonthPickerDialog(
            pickerYear = picker.year,
            visibleMonth = state.visibleMonth,
            canShowPreviousYear = picker.year > CALENDAR_YEAR_RANGE.first,
            canShowNextYear = picker.year < CALENDAR_YEAR_RANGE.last,
            onPreviousYear = { onIntent(CalendarUiIntent.ShowPreviousPickerYear) },
            onNextYear = { onIntent(CalendarUiIntent.ShowNextPickerYear) },
            onSelectMonth = { month -> onIntent(CalendarUiIntent.SelectMonth(month)) },
            onDismiss = { onIntent(CalendarUiIntent.DismissMonthPicker) },
        )
    }
}

/** 표시 월 표기이자 연·월 피커 진입점. 눌러서 이동할 수 있음을 caret 으로 드러낸다. */
@Composable
private fun VisibleMonthTitle(
    visibleMonth: YearMonth,
    onClick: () -> Unit,
) {
    val label = visibleMonth.format(VisibleMonthFormat)
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(TitleCornerRadius))
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall)
                .semantics { contentDescription = "$label, 눌러서 연·월 선택" },
        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label)
        Icon(
            painter = painterResource(UiR.drawable.ico_default_caret_right),
            contentDescription = null,
            // 아래 방향 caret 전용 에셋이 없어 오른쪽 caret 을 90도 돌려 쓴다.
            modifier =
                Modifier
                    .size(TitleCaretSize)
                    .rotate(TITLE_CARET_ROTATION),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private val VisibleMonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREA)

/** Figma 요일 헤더 ↔ 주 격자 간격. spacing 토큰에 없는 값이라 화면 로컬 상수로 둔다. */
private val CalendarHeaderSpacing = 10.dp
private val TitleCornerRadius = 8.dp
private val TitleCaretSize = 16.dp
private const val TITLE_CARET_ROTATION = 90f

@Preview(name = "기본", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenDefaultPreview() = PreviewCalendarScreen(previewMonths())

@Preview(name = "선택 · 다크", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenDarkPreview() = PreviewCalendarScreen(previewMonths(), darkTheme = true)

@Preview(name = "빈 월", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenEmptyMonthPreview() =
    PreviewCalendarScreen(
        // 기록이 없는 월도 정상 격자다. 다른 달로 계속 넘길 수 있어야 한다.
        months = mapOf(PREVIEW_MONTH to MonthlyRecordsUiContent.Records(emptyMap())),
    )

@Preview(name = "연·월 피커 열림", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenMonthPickerPreview() = PreviewCalendarScreen(previewMonths(), monthPicker = CalendarMonthPickerState(year = 2026))

@Preview(name = "월 로딩", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenLoadingPreview() = PreviewCalendarScreen(mapOf(PREVIEW_MONTH to MonthlyRecordsUiContent.Loading))

@Preview(name = "월 조회 실패", showBackground = true, heightDp = 760)
@Composable
private fun CalendarScreenLoadFailedPreview() = PreviewCalendarScreen(mapOf(PREVIEW_MONTH to MonthlyRecordsUiContent.LoadFailed))

@Composable
private fun PreviewCalendarScreen(
    months: Map<YearMonth, MonthlyRecordsUiContent>,
    monthPicker: CalendarMonthPickerState? = null,
    darkTheme: Boolean = false,
) {
    LaimoryTheme(darkTheme = darkTheme) {
        CalendarScreen(
            innerPadding = PaddingValues(),
            state =
                CalendarUiState(
                    visibleMonth = PREVIEW_MONTH,
                    selectedDate = LocalDate.of(2026, 5, 26),
                    today = LocalDate.of(2026, 5, 26),
                    months = months,
                    monthPicker = monthPicker,
                ),
            onIntent = {},
        )
    }
}

private val PREVIEW_MONTH: YearMonth = YearMonth.of(2026, 5)

/** 감정 5종 + 감정 미상을 모두 덮는 미리보기 데이터. */
private fun previewMonths(): Map<YearMonth, MonthlyRecordsUiContent> {
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
    return mapOf(PREVIEW_MONTH to MonthlyRecordsUiContent.Records(records))
}
