package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.usecase.GetMonthlyDailyRecordsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.timeline.model.CALENDAR_YEAR_RANGE
import com.soma369.laimory.feature.timeline.model.CalendarRecordUiModel
import com.soma369.laimory.feature.timeline.model.coerceToCalendarRange
import com.soma369.laimory.feature.timeline.model.toCalendarRecordsByDate
import com.soma369.laimory.feature.timeline.state.CalendarMonthPickerState
import com.soma369.laimory.feature.timeline.state.CalendarUiIntent
import com.soma369.laimory.feature.timeline.state.CalendarUiSideEffect
import com.soma369.laimory.feature.timeline.state.CalendarUiState
import com.soma369.laimory.feature.timeline.state.MonthlyRecordsUiContent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel
    @Inject
    constructor(
        private val getMonthlyDailyRecordsUseCase: GetMonthlyDailyRecordsUseCase,
        private val navigationHelper: NavigationHelper,
        private val clock: Clock,
    ) : BaseMviViewModel<CalendarUiState, CalendarUiIntent, CalendarUiSideEffect>(
            initialCalendarState(clock),
        ) {
        /** 진행 중인 월별 요청. 같은 월만 중복 차단하고 다른 월은 병렬로 나간다. */
        private val inFlightMonths = mutableSetOf<YearMonth>()

        /**
         * 캐시 무효화 세대.
         *
         * 무효화 이전에 시작된 응답이 뒤늦게 도착해 새 상태를 되살리지 못하게 막는다. 응답을 버릴 뿐
         * 요청을 취소하지는 않으므로, 세대가 다른 요청은 in-flight 집합도 건드리지 않는다.
         */
        private var generation = 0

        override suspend fun handleIntent(intent: CalendarUiIntent) {
            when (intent) {
                CalendarUiIntent.Sync -> {
                    refreshToday()
                    invalidateMonths()
                    loadAroundVisibleMonth()
                }

                is CalendarUiIntent.RetryMonth -> loadMonth(intent.month)
                is CalendarUiIntent.ShowMonth -> showMonth(intent.month)
                is CalendarUiIntent.SelectDate -> selectDate(intent.date)

                CalendarUiIntent.OpenMonthPicker ->
                    updateState { copy(monthPicker = CalendarMonthPickerState(visibleMonth.year)) }

                CalendarUiIntent.DismissMonthPicker -> updateState { copy(monthPicker = null) }
                CalendarUiIntent.ShowPreviousPickerYear -> shiftPickerYear(-1)
                CalendarUiIntent.ShowNextPickerYear -> shiftPickerYear(1)
                is CalendarUiIntent.SelectMonth -> {
                    updateState { copy(visibleMonth = intent.month.coerceToCalendarRange(), monthPicker = null) }
                    loadAroundVisibleMonth()
                }
            }
        }

        /** 화면이 떠 있는 동안 자정을 넘겼을 수 있으므로 복귀마다 오늘을 다시 계산한다. */
        private fun refreshToday() {
            val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
            updateState { if (today == this.today) this else copy(today = today) }
        }

        /**
         * 캐시를 지우지 않고 stale 로만 표시한다.
         *
         * 복귀마다 비우면 보고 있던 달이 로딩으로 깜빡이고, 재조회에 실패했을 때 유지할 "기존 내용"도
         * 남지 않는다. 표시 월과 이웃은 곧바로 재검증하고 나머지는 다시 필요해질 때 채운다.
         */
        private fun invalidateMonths() {
            generation++
            inFlightMonths.clear()
            updateState {
                copy(
                    months =
                        months.mapValues { (_, content) ->
                            if (content is MonthlyRecordsUiContent.Records) content.copy(isStale = true) else content
                        },
                )
            }
        }

        /**
         * 표시 월과 이웃 월을 함께 채운다.
         *
         * 이웃을 미리 받아 두지 않으면 스와이프가 도착한 뒤에야 요청이 나가 빈 격자가 스쳐 지나간다.
         */
        private fun loadAroundVisibleMonth() {
            val visible = state.value.visibleMonth
            listOf(visible, visible.minusMonths(1), visible.plusMonths(1)).forEach { month ->
                loadMonth(month, isVisibleMonth = month == visible)
            }
        }

        private fun loadMonth(
            month: YearMonth,
            isVisibleMonth: Boolean = month == state.value.visibleMonth,
        ) {
            if (month.year !in CALENDAR_YEAR_RANGE) return
            if (month in inFlightMonths) return
            val cached = state.value.months[month]
            // 아직 유효한 캐시는 다시 부르지 않는다. 무효화된 월은 내용을 지우지 않은 채 다시 검증한다.
            if (cached is MonthlyRecordsUiContent.Records && !cached.isStale) return
            if (cached !is MonthlyRecordsUiContent.Records) {
                updateState { copy(months = months + (month to MonthlyRecordsUiContent.Loading)) }
            }

            inFlightMonths += month
            val requestGeneration = generation
            safeLaunch(onError = { error -> handleMonthFailure(month, requestGeneration, isVisibleMonth, error) }) {
                try {
                    getMonthlyDailyRecordsUseCase(month)
                        .onSuccess { records -> applyMonthRecords(month, requestGeneration, records.toCalendarRecordsByDate()) }
                        .onFailure { error -> handleMonthFailure(month, requestGeneration, isVisibleMonth, error) }
                } finally {
                    // 세대가 지난 요청은 이미 비워진 집합을 건드리지 않는다 — 새 요청을 지워버리지 않기 위해서다.
                    if (requestGeneration == generation) inFlightMonths -= month
                }
            }
        }

        /** 응답은 해당 월 슬롯에만 기록한다. 화면은 표시 월 슬롯만 읽으므로 늦게 온 응답이 현재 화면을 덮지 않는다. */
        private fun applyMonthRecords(
            month: YearMonth,
            requestGeneration: Int,
            records: Map<LocalDate, CalendarRecordUiModel>,
        ) {
            if (requestGeneration != generation) return
            updateState { copy(months = months + (month to MonthlyRecordsUiContent.Records(records))) }
        }

        private fun handleMonthFailure(
            month: YearMonth,
            requestGeneration: Int,
            isVisibleMonth: Boolean,
            error: Throwable,
        ) {
            if (requestGeneration != generation) return
            updateState {
                // 내용이 있는 월은 실패로 비우지 않는다. 재검증에 실패해도 보고 있던 격자는 그대로 둔다.
                if (months[month] is MonthlyRecordsUiContent.Records) {
                    this
                } else {
                    copy(months = months + (month to MonthlyRecordsUiContent.LoadFailed))
                }
            }
            // 이웃 월 prefetch 실패까지 안내하면 사용자가 하지 않은 조작으로 스낵바가 뜬다. 보고 있는 달만 알린다.
            if (isVisibleMonth) handleFailure(error)
        }

        /** 월 전환은 선택 날짜를 건드리지 않는다 — TopBar 월 표기만 따라 바뀐다. */
        private fun showMonth(month: YearMonth) {
            val target = month.coerceToCalendarRange()
            updateState { if (target == visibleMonth) this else copy(visibleMonth = target) }
            loadAroundVisibleMonth()
        }

        /** 피커 연도만 넘긴다. 월을 고르지 않고 닫으면 달력은 원래 월에 그대로 있다. */
        private fun shiftPickerYear(years: Int) {
            updateState {
                val picker = monthPicker ?: return@updateState this
                val year = (picker.year + years).coerceIn(CALENDAR_YEAR_RANGE)
                if (year == picker.year) this else copy(monthPicker = picker.copy(year = year))
            }
        }

        private fun selectDate(date: LocalDate) {
            // 조회 중이거나 실패한 달은 기록 유무를 모른다. 없는 기록으로 진입해 404 를 만들지 않도록 탭을 무시한다.
            val content = state.value.contentOf(YearMonth.from(date))
            if (content !is MonthlyRecordsUiContent.Records) return

            updateState { copy(selectedDate = date) }
            // 기록이 없는 날짜는 선택 테두리만 갱신하고 네트워크·이동을 일으키지 않는다.
            if (content.recordsByDate[date] != null) {
                navigationHelper.navigateTo(TimelinePage(date))
            }
        }
    }

/**
 * 최초 진입은 기기 현지 시간대의 오늘을 선택하고 그 달을 표시한다.
 *
 * 주입되는 [Clock] 은 UTC 기준이라 현지 날짜를 얻으려면 시스템 시간대로 옮겨야 한다.
 */
private fun initialCalendarState(clock: Clock): CalendarUiState {
    val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
    return CalendarUiState(
        visibleMonth = YearMonth.from(today),
        selectedDate = today,
        today = today,
    )
}
