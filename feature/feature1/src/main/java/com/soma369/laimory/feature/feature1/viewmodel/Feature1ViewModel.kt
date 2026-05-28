package com.soma369.laimory.feature.feature1.viewmodel

import com.soma369.laimory.core.domain.usecase.GetFeature1ItemsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.feature1.state.Feature1UiIntent
import com.soma369.laimory.feature.feature1.state.Feature1UiSideEffect
import com.soma369.laimory.feature.feature1.state.Feature1UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class Feature1ViewModel
    @Inject
    constructor(
        private val getFeature1Items: GetFeature1ItemsUseCase,
    ) : BaseMviViewModel<Feature1UiState, Feature1UiIntent, Feature1UiSideEffect>(Feature1UiState()) {
        init {
            sendIntent(Feature1UiIntent.LoadItems)
        }

        override suspend fun handleIntent(intent: Feature1UiIntent) {
            when (intent) {
                Feature1UiIntent.LoadItems -> loadItems()
            }
        }

        private fun loadItems() {
            safeLaunch(
                onError = { e ->
                    updateState { copy(isLoading = false) }
                    handleException(e)
                },
            ) {
                updateState { copy(isLoading = true) }
                val items = getFeature1Items()
                updateState { copy(isLoading = false, items = items) }
            }
        }
    }
