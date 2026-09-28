package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.domain.usecase.notice.GetNoticesUseCase
import com.soma369.laimory.core.domain.usecase.notice.MarkNoticeReadUseCase
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.feature.settings.state.NoticeListContent
import com.soma369.laimory.feature.settings.state.NoticesUiIntent
import com.soma369.laimory.feature.settings.state.NoticesUiSideEffect
import com.soma369.laimory.feature.settings.state.NoticesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject

/** 설정 > 지원 > 공지사항. 목록만 그리고, 원문은 게시된 페이지를 연다. */
@HiltViewModel
class NoticesViewModel
    @Inject
    constructor(
        private val getNoticesUseCase: GetNoticesUseCase,
        private val markNoticeReadUseCase: MarkNoticeReadUseCase,
        private val navigationHelper: NavigationHelper,
    ) : BaseMviViewModel<NoticesUiState, NoticesUiIntent, NoticesUiSideEffect>(
            NoticesUiState(),
        ) {
        private var syncJob: Job? = null

        override suspend fun handleIntent(intent: NoticesUiIntent) {
            when (intent) {
                NoticesUiIntent.Sync -> sync()
                is NoticesUiIntent.NoticeClicked -> open(intent.notice)
                NoticesUiIntent.NavigateBack -> navigationHelper.navigateToBack()
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
                    if (state.value.content !is NoticeListContent.Items) {
                        updateState { copy(content = NoticeListContent.Loading) }
                    }
                    getNoticesUseCase()
                        .onSuccess { feed ->
                            updateState {
                                copy(
                                    content =
                                        if (feed.notices.isEmpty()) {
                                            NoticeListContent.Empty
                                        } else {
                                            NoticeListContent.Items(feed.notices, feed.newIds)
                                        },
                                )
                            }
                        }.onFailure { error ->
                            markFailure()
                            handleFailure(error)
                        }
                }
        }

        /** 원문을 먼저 열고 읽음은 뒤에 남긴다 — 저장이 늦거나 실패해도 여는 것을 막지 않는다. */
        private suspend fun open(notice: Notice) {
            sendEffect(NoticesUiSideEffect.OpenContent(notice.contentUrl))
            val items = state.value.content as? NoticeListContent.Items ?: return
            if (notice.id in items.newIds) {
                updateState { copy(content = items.copy(newIds = items.newIds - notice.id)) }
            }
            // 표시가 없던 공지도 남긴다. 읽음 기록을 못 읽어 표시를 비워 둔 경우에도 다음엔 맞게 뜬다.
            markNoticeReadUseCase(notice, items.notices)
        }

        /** 보여 주던 목록이 있으면 실패로 지우지 않는다 — 갱신에 실패했을 뿐 목록은 아직 유효하다. */
        private fun markFailure() {
            updateState {
                if (content is NoticeListContent.Items) this else copy(content = NoticeListContent.LoadFailed)
            }
        }
    }
