package com.soma369.laimory.feature.collection.viewmodel

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.usecase.ClearCollectedCalendarUseCase
import com.soma369.laimory.core.domain.usecase.CollectCalendarUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.collection.state.CalendarUiIntent
import com.soma369.laimory.feature.collection.state.CalendarUiSideEffect
import com.soma369.laimory.feature.collection.state.CalendarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CalendarCollectionViewModel
    @Inject
    constructor(
        observeSourceItemsUseCase: ObserveSourceItemsUseCase,
        private val collectCalendarUseCase: CollectCalendarUseCase,
        private val clearCollectedCalendarUseCase: ClearCollectedCalendarUseCase,
    ) : BaseMviViewModel<CalendarUiState, CalendarUiIntent, CalendarUiSideEffect>(CalendarUiState()) {
        init {
            safeLaunch {
                observeSourceItemsUseCase().collect { items ->
                    updateState {
                        copy(
                            isLoading = false,
                            stagedEvents = items.filter { it.itemType == ItemType.CALENDAR },
                        )
                    }
                }
            }
        }

        override suspend fun handleIntent(intent: CalendarUiIntent) {
            when (intent) {
                CalendarUiIntent.Collect -> collect()
                CalendarUiIntent.ClearStaged -> clearStaged()
            }
        }

        private fun collect() =
            safeLaunch(onError = ::onBusyError) {
                updateState { copy(isBusy = true) }
                val inserted = collectCalendarUseCase()
                updateState { copy(isBusy = false) }
                sendEffect(CalendarUiSideEffect.ShowMessage("일정 수집 완료 — 새로 저장 ${inserted}건"))
            }

        private fun clearStaged() =
            safeLaunch {
                clearCollectedCalendarUseCase()
                sendEffect(CalendarUiSideEffect.ShowMessage("스테이징 일정을 모두 비웠습니다."))
            }

        /** 진행(isBusy) 중 실패 시 표시를 원복한 뒤 공통 실패 처리로 넘긴다. */
        private fun onBusyError(e: Throwable) {
            updateState { copy(isBusy = false) }
            handleFailure(e)
        }
    }
