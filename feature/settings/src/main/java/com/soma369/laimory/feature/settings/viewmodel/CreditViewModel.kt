package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsBillingState
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.usecase.credit.GetTimelineCreditUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.settings.credit.CreditBalanceStore
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
 *
 * 표시값은 [CreditBalanceStore] 를 따른다. 들어오면 기억해 둔 값을 바로 보이고 뒤에서 다시 받는다. 이 ViewModel 은 Activity
 * 수명이지만 저장소가 계정 경계에서 비워지므로 다른 계정의 잔액이 비치지 않는다.
 */
@HiltViewModel
class CreditViewModel
    @Inject
    constructor(
        private val getTimelineCredit: GetTimelineCreditUseCase,
        private val balanceStore: CreditBalanceStore,
        private val navigationHelper: NavigationHelper,
        private val analyticsHelper: AnalyticsHelper,
    ) : BaseMviViewModel<CreditUiState, CreditUiIntent, CreditUiSideEffect>(CreditUiState()) {
        /** 마지막 조회 번호. 먼저 보낸 조회가 늦게 끝나도 새 결과를 덮지 않는다. */
        private var request = 0

        init {
            safeLaunch {
                balanceStore.balance.collect { balance ->
                    updateState { copy(remaining = balance.remaining, cost = balance.cost) }
                }
            }
        }

        override suspend fun handleIntent(intent: CreditUiIntent) {
            when (intent) {
                CreditUiIntent.Refresh -> refresh()
                CreditUiIntent.NavigateBack -> navigationHelper.navigateToBack()
                is CreditUiIntent.PackageClicked -> {
                    // 충전을 열기 전의 수요다. 결제를 열면 그때의 상태와 결과 이벤트를 따로 정한다.
                    analyticsHelper.log(AnalyticsEvent.CreditPackageClicked(intent.amount, AnalyticsBillingState.COMING_SOON))
                    sendEffect(CreditUiSideEffect.ShowSnackbar(CHARGE_NOT_OPEN_MESSAGE))
                }
            }
        }

        /** 들어올 때마다 다시 받는다. 보이던 값은 비우지 않는다 — 응답이 오면 저장소를 거쳐 조용히 바뀐다. */
        private fun refresh() {
            val current = ++request
            val token = balanceStore.beginRequest()
            updateState { copy(isLoading = true) }
            safeLaunch {
                val credit = getTimelineCredit()
                if (current != request) return@safeLaunch
                balanceStore.updateCredit(token, credit)
                updateState { copy(isLoading = false) }
            }
        }

        companion object {
            const val CHARGE_NOT_OPEN_MESSAGE = "크레딧 충전은 준비 중이에요"
        }
    }
