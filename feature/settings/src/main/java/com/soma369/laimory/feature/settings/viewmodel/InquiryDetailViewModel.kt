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

        /** 새 진입([InquiryDetailUiIntent.Opened])이나 다른 문의로 바뀔 때마다 올린다. 이전 요청 결과는 버린다. */
        private var generation = 0

        override suspend fun handleIntent(intent: InquiryDetailUiIntent) {
            when (intent) {
                is InquiryDetailUiIntent.Opened -> {
                    generation++
                    loadJob?.cancel()
                    updateState { InquiryDetailUiState() }
                    load(intent.inquiryId)
                }
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
                generation++
                loadJob?.cancel()
                updateState { InquiryDetailUiState(inquiryId = inquiryId, content = InquiryDetailContent.Loading) }
            } else if (loadJob?.isActive == true) {
                return
            } else if (state.value.content !is InquiryDetailContent.Loaded) {
                updateState { copy(content = InquiryDetailContent.Loading) }
            }
            val requestGeneration = generation
            loadJob =
                safeLaunch(onError = { markFailure(requestGeneration, it) }) {
                    getInquiryDetailUseCase(inquiryId)
                        .onSuccess { detail ->
                            if (requestGeneration == generation) {
                                updateState { copy(content = InquiryDetailContent.Loaded(detail)) }
                            }
                        }.onFailure { markFailure(requestGeneration, it) }
                }
        }

        /** 보여 주던 내용이 있으면 갱신 실패로 지우지 않는다. 찾을 수 없게 된 것은 바로 알린다. */
        private fun markFailure(
            requestGeneration: Int,
            error: Throwable,
        ) {
            // 다른 문의를 열며 끊었거나 이전 진입에서 시작한 요청이다. 지금 화면의 실패가 아니다.
            if (error is CancellationException || requestGeneration != generation) return
            updateState {
                when {
                    error is InquiryNotFoundException -> copy(content = InquiryDetailContent.NotFound)
                    content is InquiryDetailContent.Loaded -> this
                    else -> copy(content = InquiryDetailContent.LoadFailed)
                }
            }
            if (error !is InquiryNotFoundException) handleFailure(error)
        }
    }
