package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.navigation.TimelineEventEditorPage
import com.soma369.laimory.core.domain.usecase.ObserveTimelineRecordUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.timeline.model.toUiModel
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiContent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiIntent
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiSideEffect
import com.soma369.laimory.feature.timeline.state.TimelineRecordUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TimelineRecordViewModel
    @Inject
    constructor(
        observeTimelineRecordUseCase: ObserveTimelineRecordUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<TimelineRecordUiState, TimelineRecordUiIntent, TimelineRecordUiSideEffect>(
            TimelineRecordUiState(),
        ) {
        init {
            safeLaunch {
                observeTimelineRecordUseCase().collect { timeline ->
                    updateState {
                        copy(
                            content =
                                timeline
                                    ?.toUiModel()
                                    ?.let(TimelineRecordUiContent::Record)
                                    ?: TimelineRecordUiContent.Unavailable,
                        )
                    }
                }
            }
        }

        override suspend fun handleIntent(intent: TimelineRecordUiIntent) {
            when (intent) {
                TimelineRecordUiIntent.NavigateBack -> navigationHelper.navigateToBack()
                TimelineRecordUiIntent.OpenRecordMenu ->
                    sendEffect(TimelineRecordUiSideEffect.OpenRecordMenu)
                is TimelineRecordUiIntent.SelectEvent ->
                    navigationHelper.navigateTo(TimelineEventEditorPage(intent.timelineEventId))
            }
        }
    }
