package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.navigation.InquiryDetailPage
import com.soma369.laimory.core.domain.usecase.inquiry.GetMyInquiriesUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.settings.state.InquiriesUiIntent
import com.soma369.laimory.feature.settings.state.InquiriesUiSideEffect
import com.soma369.laimory.feature.settings.state.InquiriesUiState
import com.soma369.laimory.feature.settings.state.InquiryListContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject

/** 설정 > 지원 > 문의 내역. 내가 보낸 문의와 처리 상태를 훑고, 한 건을 누르면 상세로 간다. */
@HiltViewModel
class InquiriesViewModel
    @Inject
    constructor(
        private val getMyInquiriesUseCase: GetMyInquiriesUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<InquiriesUiState, InquiriesUiIntent, InquiriesUiSideEffect>(InquiriesUiState()) {
        private var syncJob: Job? = null

        override suspend fun handleIntent(intent: InquiriesUiIntent) {
            when (intent) {
                InquiriesUiIntent.Sync -> sync()
                is InquiriesUiIntent.InquiryClicked -> navigationHelper.navigateTo(InquiryDetailPage(intent.inquiryId))
                InquiriesUiIntent.NavigateBack -> navigationHelper.navigateToBack()
            }
        }

        private fun sync() {
            if (syncJob?.isActive == true) return
            syncJob =
                safeLaunch(
                    onError = {
                        markFailure()
                        handleFailure(it)
                    },
                ) {
                    if (state.value.content !is InquiryListContent.Items) {
                        updateState { copy(content = InquiryListContent.Loading) }
                    }
                    getMyInquiriesUseCase()
                        .onSuccess { inquiries ->
                            updateState {
                                copy(
                                    content =
                                        if (inquiries.isEmpty()) {
                                            InquiryListContent.Empty
                                        } else {
                                            InquiryListContent.Items(inquiries)
                                        },
                                )
                            }
                        }.onFailure { error ->
                            markFailure()
                            handleFailure(error)
                        }
                }
        }

        /** 보여 주던 목록이 있으면 실패로 지우지 않는다 — 갱신에 실패했을 뿐 목록은 아직 유효하다. */
        private fun markFailure() {
            updateState {
                if (content is InquiryListContent.Items) this else copy(content = InquiryListContent.LoadFailed)
            }
        }
    }
