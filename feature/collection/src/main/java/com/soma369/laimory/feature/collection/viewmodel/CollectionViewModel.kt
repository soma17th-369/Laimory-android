package com.soma369.laimory.feature.collection.viewmodel

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.usecase.ClearCollectedPhotosUseCase
import com.soma369.laimory.core.domain.usecase.CollectSelectedPhotosUseCase
import com.soma369.laimory.core.domain.usecase.GetPhotosOnDateUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.collection.state.CollectionUiIntent
import com.soma369.laimory.feature.collection.state.CollectionUiSideEffect
import com.soma369.laimory.feature.collection.state.CollectionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel
    @Inject
    constructor(
        observeSourceItemsUseCase: ObserveSourceItemsUseCase,
        private val getPhotosOnDateUseCase: GetPhotosOnDateUseCase,
        private val collectSelectedPhotosUseCase: CollectSelectedPhotosUseCase,
        private val clearCollectedPhotosUseCase: ClearCollectedPhotosUseCase,
    ) : BaseMviViewModel<CollectionUiState, CollectionUiIntent, CollectionUiSideEffect>(CollectionUiState()) {
        init {
            safeLaunch {
                observeSourceItemsUseCase().collect { items ->
                    updateState {
                        copy(
                            isLoading = false,
                            stagedPhotos = items.filter { it.itemType == ItemType.PHOTO },
                        )
                    }
                }
            }
        }

        override suspend fun handleIntent(intent: CollectionUiIntent) {
            when (intent) {
                is CollectionUiIntent.SelectDate -> loadCandidates(intent.date)
                CollectionUiIntent.CollectAllOnDate -> collectPhotos(state.value.candidates.map { it.id })
                CollectionUiIntent.OpenSelectSheet -> updateState { copy(isSheetVisible = true, selectedIds = emptySet()) }
                CollectionUiIntent.DismissSelectSheet -> updateState { copy(isSheetVisible = false) }
                is CollectionUiIntent.ToggleCandidate -> toggleCandidate(intent.id)
                CollectionUiIntent.CollectSelected -> collectPhotos(state.value.selectedIds.toList())
                CollectionUiIntent.ClearStaged -> clearStaged()
            }
        }

        private fun loadCandidates(date: LocalDate) =
            safeLaunch(onError = ::onBusyError) {
                updateState { copy(isBusy = true) }
                val candidates = getPhotosOnDateUseCase(date)
                updateState { copy(selectedDate = date, candidates = candidates, isBusy = false) }
                if (candidates.isEmpty()) {
                    sendEffect(CollectionUiSideEffect.ShowMessage("$date 에 촬영된 사진이 없습니다."))
                }
            }

        private fun collectPhotos(ids: List<Long>) =
            safeLaunch(onError = ::onBusyError) {
                if (ids.isEmpty()) {
                    sendEffect(CollectionUiSideEffect.ShowMessage("선택된 사진이 없습니다."))
                    return@safeLaunch
                }
                updateState { copy(isBusy = true) }
                val inserted = collectSelectedPhotosUseCase(ids)
                updateState { copy(isBusy = false, isSheetVisible = false, selectedIds = emptySet()) }
                sendEffect(CollectionUiSideEffect.ShowMessage("사진 수집 완료 — 새로 저장 ${inserted}건 / 요청 ${ids.size}건"))
            }

        private fun toggleCandidate(id: Long) =
            updateState {
                copy(selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id)
            }

        private fun clearStaged() =
            safeLaunch {
                clearCollectedPhotosUseCase()
                sendEffect(CollectionUiSideEffect.ShowMessage("스테이징 사진을 모두 비웠습니다."))
            }

        /** 진행(isBusy) 중 실패 시 표시를 원복한 뒤 공통 실패 처리로 넘긴다. */
        private fun onBusyError(e: Throwable) {
            updateState { copy(isBusy = false) }
            handleFailure(e)
        }
    }
