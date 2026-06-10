package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.usecase.GetIntroInfoUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiSideEffect
import com.soma369.laimory.feature.home.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val getIntroInfoUseCase: GetIntroInfoUseCase,
    ) : BaseMviViewModel<HomeUiState, HomeUiIntent, HomeUiSideEffect>(HomeUiState()) {
        init {
            sendIntent(HomeUiIntent.LoadIntroInfo)
        }

        override suspend fun handleIntent(intent: HomeUiIntent) {
            when (intent) {
                HomeUiIntent.Increment -> updateState { copy(counter = counter + 1) }
                HomeUiIntent.Decrement -> updateState { copy(counter = counter - 1) }
                HomeUiIntent.ShowToast -> sendEffect(HomeUiSideEffect.ShowToast("현재 카운터: ${state.value.counter}"))
                HomeUiIntent.LoadIntroInfo -> loadIntroInfo()
            }
        }

        private fun loadIntroInfo() {
            safeLaunch(
                onError = { e ->
                    updateState { copy(isLoading = false) }
                    handleException(e)
                },
            ) {
                updateState { copy(isLoading = true) }
                val introInfo = getIntroInfoUseCase()
                updateState { copy(introInfo = introInfo, isLoading = false) }
            }
        }
    }
