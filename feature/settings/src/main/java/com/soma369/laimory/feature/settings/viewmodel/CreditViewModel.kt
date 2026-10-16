package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.usecase.credit.GetTimelineCreditUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.settings.state.CreditBalance
import com.soma369.laimory.feature.settings.state.CreditUiIntent
import com.soma369.laimory.feature.settings.state.CreditUiSideEffect
import com.soma369.laimory.feature.settings.state.CreditUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 남은 크레딧과 충전 상품.
 *
 * 충전은 아직 열지 않았다 — 상품을 누르면 준비 중이라고만 알린다. 잔액과 함께 1개당 쓰임(비용)도 받는다. 비용은 서버 값이라
 * `1개로 1번` 을 앱에 고정하지 않는다.
 */
@HiltViewModel
class CreditViewModel
    @Inject
    constructor(
        private val getTimelineCredit: GetTimelineCreditUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<CreditUiState, CreditUiIntent, CreditUiSideEffect>(CreditUiState()) {
        /** 마지막 조회 번호. 먼저 보낸 조회가 늦게 끝나도 새 결과를 덮지 않는다. */
        private var request = 0

        override suspend fun handleIntent(intent: CreditUiIntent) {
            when (intent) {
                CreditUiIntent.Refresh -> refresh()
                CreditUiIntent.NavigateBack -> navigationHelper.navigateToBack()
                is CreditUiIntent.PackageClicked -> sendEffect(CreditUiSideEffect.ShowSnackbar(CHARGE_NOT_OPEN_MESSAGE))
            }
        }

        /**
         * 들어올 때마다 받는 중으로 되돌린다. 이 ViewModel 은 Activity 수명이라, 이전 값을 남기면 다른 계정으로 다시 들어와도
         * 옛 잔액이 잠깐 보인다.
         */
        private fun refresh() {
            val current = ++request
            updateState { copy(balance = CreditBalance.Loading) }
            safeLaunch {
                val balance = getTimelineCredit()?.let(CreditBalance::Loaded) ?: CreditBalance.Unavailable
                if (current == request) updateState { copy(balance = balance) }
            }
        }

        companion object {
            const val CHARGE_NOT_OPEN_MESSAGE = "크레딧 충전은 준비 중이에요"
        }
    }
