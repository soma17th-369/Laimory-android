package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface NoticesUiSideEffect : UiSideEffect {
    /**
     * 게시된 원문 페이지를 연다. 브라우저를 띄우는 일은 Context 가 있는 화면이 맡고, 실제로 열렸으면
     * [NoticesUiIntent.NoticeOpened] 로 알려 준다 — 열지 못한 공지를 읽었다고 남기지 않는다.
     */
    data class OpenContent(
        val notice: Notice,
    ) : NoticesUiSideEffect

    data class ShowSnackbar(
        val message: String,
    ) : NoticesUiSideEffect
}
