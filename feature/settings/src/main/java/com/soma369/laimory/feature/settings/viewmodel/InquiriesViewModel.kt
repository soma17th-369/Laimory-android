package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.exception.HandledException
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
import kotlin.coroutines.cancellation.CancellationException

/** 문의 화면의 `문의 내역` 탭. 내가 보낸 문의와 처리 상태를 훑고, 한 건을 누르면 상세로 간다. */
@HiltViewModel
class InquiriesViewModel
    @Inject
    constructor(
        private val getMyInquiriesUseCase: GetMyInquiriesUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<InquiriesUiState, InquiriesUiIntent, InquiriesUiSideEffect>(InquiriesUiState()) {
        private var syncJob: Job? = null

        /** [InquiriesUiIntent.Opened] 마다 올린다. 이전 진입에서 시작한 요청의 결과는 버린다. */
        private var generation = 0

        override suspend fun handleIntent(intent: InquiriesUiIntent) {
            when (intent) {
                InquiriesUiIntent.Opened -> reset()
                InquiriesUiIntent.Sync -> sync()
                is InquiriesUiIntent.InquiryClicked -> navigationHelper.navigateTo(InquiryDetailPage(intent.inquiryId))
            }
        }

        private fun reset() {
            generation++
            syncJob?.cancel()
            syncJob = null
            updateState { InquiriesUiState() }
        }

        private fun sync() {
            if (syncJob?.isActive == true) return
            val requestGeneration = generation
            syncJob =
                safeLaunch(onError = { onSyncFailed(requestGeneration, it) }) {
                    if (state.value.content !is InquiryListContent.Items) {
                        updateState { copy(content = InquiryListContent.Loading) }
                    }
                    getMyInquiriesUseCase()
                        .onSuccess { inquiries ->
                            if (requestGeneration != generation) return@onSuccess
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
                        }.onFailure { onSyncFailed(requestGeneration, it) }
                }
        }

        /**
         * 보여 주던 목록이 있으면 실패로 지우지 않는다 — 갱신에 실패했을 뿐 목록은 아직 유효하다.
         * 대신 새로 받지 못했다는 것은 알린다. 알리지 않으면 오래된 상태를 최신처럼 믿게 된다.
         */
        private fun onSyncFailed(
            requestGeneration: Int,
            error: Throwable,
        ) {
            if (requestGeneration != generation || error is CancellationException) return
            handleFailure(error)
            if (state.value.content !is InquiryListContent.Items) {
                updateState { copy(content = InquiryListContent.LoadFailed) }
                return
            }
            // 세션 만료·서버 오류는 공용 안내가 이미 떴다.
            if (error !is HandledException) {
                sendEffect(InquiriesUiSideEffect.ShowSnackbar("문의 내역을 새로 불러오지 못했어요. 잠시 후 다시 시도해 주세요."))
            }
        }
    }
