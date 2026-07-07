package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.navigation.CollectionPage
import com.soma369.laimory.core.domain.navigation.Feature1Page
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
        private val navigationHelper: NavigationHelper,
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
                HomeUiIntent.NavigateToFeature1 -> navigationHelper.navigateTo(Feature1Page)
                HomeUiIntent.NavigateToCollection -> navigationHelper.navigateTo(CollectionPage)
            }
        }

        private fun loadIntroInfo() =
            safeLaunch {
                updateState { copy(isLoading = true) }
                getIntroInfoUseCase()
                    .onSuccess { info -> updateState { copy(introInfo = info, isLoading = false) } }
                    .onFailure { e ->
                        updateState { copy(isLoading = false) }
                        handleFailure(e)
                    }
            }
    }
