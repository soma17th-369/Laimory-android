package com.soma369.laimory.feature.collection.viewmodel

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.usecase.ClearCollectedHealthUseCase
import com.soma369.laimory.core.domain.usecase.CollectHealthUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.collection.state.HealthUiIntent
import com.soma369.laimory.feature.collection.state.HealthUiSideEffect
import com.soma369.laimory.feature.collection.state.HealthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HealthCollectionViewModel
    @Inject
    constructor(
        observeSourceItemsUseCase: ObserveSourceItemsUseCase,
        private val collectHealthUseCase: CollectHealthUseCase,
        private val clearCollectedHealthUseCase: ClearCollectedHealthUseCase,
    ) : BaseMviViewModel<HealthUiState, HealthUiIntent, HealthUiSideEffect>(HealthUiState()) {
        init {
            safeLaunch {
                observeSourceItemsUseCase().collect { items ->
                    updateState {
                        copy(
                            isLoading = false,
                            stagedHealth = items.filter { it.itemType == ItemType.HEALTH },
                        )
                    }
                }
            }
        }

        override suspend fun handleIntent(intent: HealthUiIntent) {
            when (intent) {
                HealthUiIntent.Collect -> collect()
                HealthUiIntent.ClearStaged -> clearStaged()
            }
        }

        private fun collect() =
            safeLaunch(onError = ::onBusyError) {
                updateState { copy(isBusy = true) }
                val inserted = collectHealthUseCase()
                updateState { copy(isBusy = false) }
                sendEffect(HealthUiSideEffect.ShowMessage("건강 데이터 수집 완료 — 새로 저장 ${inserted}건"))
            }

        private fun clearStaged() =
            safeLaunch {
                clearCollectedHealthUseCase()
                sendEffect(HealthUiSideEffect.ShowMessage("스테이징 건강 데이터를 모두 비웠습니다."))
            }

        /** 진행(isBusy) 중 실패 시 표시를 원복한 뒤 공통 실패 처리로 넘긴다. */
        private fun onBusyError(e: Throwable) {
            updateState { copy(isBusy = false) }
            handleFailure(e)
        }
    }
