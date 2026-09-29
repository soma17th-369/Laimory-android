package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.InquiryNotFoundException
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.usecase.inquiry.GetInquiryDetailUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.settings.state.InquiryDetailContent
import com.soma369.laimory.feature.settings.state.InquiryDetailUiIntent
import com.soma369.laimory.feature.settings.state.InquiryDetailUiSideEffect
import com.soma369.laimory.feature.settings.state.InquiryDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 문의 내역의 한 건. 보낸 내용과 처리 상태를 보여 준다.
 *
 * ViewModel 이 Activity 수명이라 문의를 바꿔 열어도 같은 인스턴스가 온다. 다른 문의를 열면 이전 내용을
 * 먼저 지우고 불러온다 — 잠깐이라도 다른 문의 내용이 보이면 안 된다.
 */
@HiltViewModel
class InquiryDetailViewModel
    @Inject
    constructor(
        private val getInquiryDetailUseCase: GetInquiryDetailUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<InquiryDetailUiState, InquiryDetailUiIntent, InquiryDetailUiSideEffect>(InquiryDetailUiState()) {
        private var loadJob: Job? = null

        override suspend fun handleIntent(intent: InquiryDetailUiIntent) {
            when (intent) {
                is InquiryDetailUiIntent.Load -> load(intent.inquiryId)
                InquiryDetailUiIntent.Retry -> load(state.value.inquiryId)
                InquiryDetailUiIntent.NavigateBack -> navigationHelper.navigateToBack()
            }
        }

        private fun load(inquiryId: Long?) {
            if (inquiryId == null) {
                loadJob?.cancel()
                updateState { InquiryDetailUiState(content = InquiryDetailContent.NotFound) }
                return
            }
            if (state.value.inquiryId != inquiryId) {
                // 이전 문의를 불러오던 중이면 그 결과가 새 문의 자리에 들어오지 않게 끊는다.
                loadJob?.cancel()
                updateState { InquiryDetailUiState(inquiryId = inquiryId, content = InquiryDetailContent.Loading) }
            } else if (loadJob?.isActive == true) {
                return
            } else if (state.value.content !is InquiryDetailContent.Loaded) {
                updateState { copy(content = InquiryDetailContent.Loading) }
            }
            loadJob =
                safeLaunch(onError = { markFailure(inquiryId, it) }) {
                    getInquiryDetailUseCase(inquiryId)
                        .onSuccess { detail ->
                            updateState {
                                if (this.inquiryId == inquiryId) copy(content = InquiryDetailContent.Loaded(detail)) else this
                            }
                        }.onFailure { markFailure(inquiryId, it) }
                }
        }

        /** 보여 주던 내용이 있으면 갱신 실패로 지우지 않는다. 찾을 수 없게 된 것은 바로 알린다. */
        private fun markFailure(
            inquiryId: Long,
            error: Throwable,
        ) {
            // 다른 문의를 열며 끊은 요청이다. 실패가 아니다.
            if (error is CancellationException) return
            updateState {
                when {
                    this.inquiryId != inquiryId -> this
                    error is InquiryNotFoundException -> copy(content = InquiryDetailContent.NotFound)
                    content is InquiryDetailContent.Loaded -> this
                    else -> copy(content = InquiryDetailContent.LoadFailed)
                }
            }
            if (error !is InquiryNotFoundException) handleFailure(error)
        }
    }
