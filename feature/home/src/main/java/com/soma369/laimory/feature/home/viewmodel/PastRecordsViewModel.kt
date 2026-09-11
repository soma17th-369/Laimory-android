package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.usecase.GetDailyRecordsUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.home.model.toPastRecordUiModel
import com.soma369.laimory.feature.home.state.PastRecordsContent
import com.soma369.laimory.feature.home.state.PastRecordsUiIntent
import com.soma369.laimory.feature.home.state.PastRecordsUiSideEffect
import com.soma369.laimory.feature.home.state.PastRecordsUiState
import com.soma369.laimory.feature.home.state.toMonthGroups
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject

/**
 * 저장된 지난 기록 목록.
 *
 * 홈이 갖고 있던 조회·상태를 그대로 옮겼다. 홈은 이제 오늘의 원천과 만들기에만 집중한다.
 */
@HiltViewModel
class PastRecordsViewModel
    @Inject
    constructor(
        private val getDailyRecordsUseCase: GetDailyRecordsUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<PastRecordsUiState, PastRecordsUiIntent, PastRecordsUiSideEffect>(
            PastRecordsUiState(),
        ) {
        private var syncJob: Job? = null

        override suspend fun handleIntent(intent: PastRecordsUiIntent) {
            when (intent) {
                PastRecordsUiIntent.Sync -> sync()
                is PastRecordsUiIntent.SelectRecord -> navigationHelper.navigateTo(TimelinePage(intent.recordDate))
                PastRecordsUiIntent.NavigateBack -> navigationHelper.navigateToBack()
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
                    // 이미 목록을 보여주는 중이면 유지한 채 재동기화한다. (깜빡임 방지)
                    if (state.value.content !is PastRecordsContent.Groups) {
                        updateState { copy(content = PastRecordsContent.Loading) }
                    }
                    getDailyRecordsUseCase()
                        .onSuccess { timelines ->
                            updateState {
                                copy(
                                    content =
                                        if (timelines.isEmpty()) {
                                            PastRecordsContent.Empty
                                        } else {
                                            PastRecordsContent.Groups(
                                                timelines.map(DailyTimeline::toPastRecordUiModel).toMonthGroups(),
                                            )
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
                if (content is PastRecordsContent.Groups) this else copy(content = PastRecordsContent.LoadFailed)
            }
        }
    }
