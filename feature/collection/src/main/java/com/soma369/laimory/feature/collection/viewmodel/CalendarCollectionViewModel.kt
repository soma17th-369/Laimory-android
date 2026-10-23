package com.soma369.laimory.feature.collection.viewmodel

import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.usecase.GetCalendarEventsInWindowUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.collection.state.CalendarUiIntent
import com.soma369.laimory.feature.collection.state.CalendarUiSideEffect
import com.soma369.laimory.feature.collection.state.CalendarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 수집 실험실 "일정" 탭. 일정은 저장하지 않으므로 홈과 같은 조회 경로로 최근 한 달을 읽어 보여 준다.
 */
@HiltViewModel
class CalendarCollectionViewModel
    @Inject
    constructor(
        private val getCalendarEventsInWindow: GetCalendarEventsInWindowUseCase,
    ) : BaseMviViewModel<CalendarUiState, CalendarUiIntent, CalendarUiSideEffect>(CalendarUiState()) {
        override suspend fun handleIntent(intent: CalendarUiIntent) {
            when (intent) {
                CalendarUiIntent.Load -> load()
            }
        }

        private fun load() =
            safeLaunch(onError = ::onBusyError) {
                updateState { copy(isBusy = true) }
                val events = getCalendarEventsInWindow(recentWindow())
                updateState { copy(isBusy = false, hasLoaded = true, events = events) }
                sendEffect(CalendarUiSideEffect.ShowMessage("일정 조회 완료 — ${events.size}건"))
            }

        /** 오늘 기준 최근 [RECENT_MONTHS]개월. 끝은 내일 자정이라 아직 시작하지 않은 오늘 일정까지 보인다. */
        private fun recentWindow(): RecordDateWindow {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            return RecordDateWindow(
                start = today.minusMonths(RECENT_MONTHS).atStartOfDay(zone).toInstant(),
                end = today.plusDays(1).atStartOfDay(zone).toInstant(),
            )
        }

        /** 진행(isBusy) 중 실패 시 표시를 원복한 뒤 공통 실패 처리로 넘긴다. */
        private fun onBusyError(e: Throwable) {
            updateState { copy(isBusy = false) }
            handleFailure(e)
        }

        private companion object {
            const val RECENT_MONTHS = 1L
        }
    }
