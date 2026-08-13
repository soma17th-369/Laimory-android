package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.usecase.GetDailyRecordsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.timeline.model.toCalendarRecordsByDate
import com.soma369.laimory.feature.timeline.state.CalendarMonthPickerState
import com.soma369.laimory.feature.timeline.state.CalendarRecordsUiContent
import com.soma369.laimory.feature.timeline.state.CalendarUiIntent
import com.soma369.laimory.feature.timeline.state.CalendarUiSideEffect
import com.soma369.laimory.feature.timeline.state.CalendarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel
    @Inject
    constructor(
        private val getDailyRecordsUseCase: GetDailyRecordsUseCase,
        private val navigationHelper: NavigationHelper,
        private val clock: Clock,
    ) : BaseMviViewModel<CalendarUiState, CalendarUiIntent, CalendarUiSideEffect>(
            initialCalendarState(clock),
        ) {
        private var syncJob: Job? = null

        override suspend fun handleIntent(intent: CalendarUiIntent) {
            when (intent) {
                CalendarUiIntent.Sync -> {
                    refreshToday()
                    syncRecords()
                }

                CalendarUiIntent.RetryLoad -> syncRecords()
                is CalendarUiIntent.ShowMonth -> showMonth(intent.month)
                is CalendarUiIntent.SelectDate -> selectDate(intent.date)

                CalendarUiIntent.OpenMonthPicker ->
                    updateState { copy(monthPicker = CalendarMonthPickerState(visibleMonth.year)) }

                CalendarUiIntent.DismissMonthPicker -> updateState { copy(monthPicker = null) }
                CalendarUiIntent.ShowPreviousPickerYear -> shiftPickerYear(-1)
                CalendarUiIntent.ShowNextPickerYear -> shiftPickerYear(1)
                is CalendarUiIntent.SelectMonth ->
                    updateState { copy(visibleMonth = intent.month, monthPicker = null) }
            }
        }

        /** 화면이 떠 있는 동안 자정을 넘겼을 수 있으므로 복귀마다 오늘을 다시 계산한다. */
        private fun refreshToday() {
            val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
            updateState { if (today == this.today) this else copy(today = today) }
        }

        /** 서버 전체 기록을 동기화한다. 진행 중이면 중복 요청하지 않는다. */
        private fun syncRecords() {
            if (syncJob?.isActive == true) return
            syncJob =
                safeLaunch(
                    onError = {
                        markLoadFailure()
                        handleFailure(it)
                    },
                ) {
                    // 이미 격자를 보여주는 중이면 유지한 채 재동기화한다. (깜빡임 방지)
                    if (state.value.content !is CalendarRecordsUiContent.Content) {
                        updateState { copy(content = CalendarRecordsUiContent.Loading) }
                    }
                    getDailyRecordsUseCase()
                        .onSuccess { timelines ->
                            updateState {
                                copy(
                                    content =
                                        if (timelines.isEmpty()) {
                                            CalendarRecordsUiContent.Empty
                                        } else {
                                            CalendarRecordsUiContent.Content(timelines.toCalendarRecordsByDate())
                                        },
                                )
                            }
                        }.onFailure { error ->
                            markLoadFailure()
                            handleFailure(error)
                        }
                }
        }

        /** 이미 표시 중인 격자가 있으면 실패로 지우지 않는다. 최초 조회 실패만 전체 오류로 표면화한다. */
        private fun markLoadFailure() {
            updateState {
                if (content is CalendarRecordsUiContent.Content) {
                    this
                } else {
                    copy(content = CalendarRecordsUiContent.LoadFailed)
                }
            }
        }

        /** 월 전환은 선택 날짜를 건드리지 않는다 — TopBar 월 표기만 따라 바뀐다. */
        private fun showMonth(month: YearMonth) {
            updateState { if (month == visibleMonth) this else copy(visibleMonth = month) }
        }

        /** 피커 연도만 넘긴다. 월을 고르지 않고 닫으면 달력은 원래 월에 그대로 있다. */
        private fun shiftPickerYear(years: Int) {
            updateState {
                val picker = monthPicker ?: return@updateState this
                copy(monthPicker = picker.copy(year = picker.year + years))
            }
        }

        private fun selectDate(date: LocalDate) {
            updateState { copy(selectedDate = date) }
            // 기록이 없는 날짜는 선택 테두리만 갱신하고 네트워크·이동을 일으키지 않는다.
            val hasRecord = state.value.recordOf(date) != null
            if (hasRecord) {
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
