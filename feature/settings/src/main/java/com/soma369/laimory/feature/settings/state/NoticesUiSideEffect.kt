package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface NoticesUiSideEffect : UiSideEffect {
    /** 게시된 원문 페이지를 연다. 브라우저를 띄우는 일은 Context 가 있는 화면이 맡는다. */
    data class OpenContent(
        val url: String,
    ) : NoticesUiSideEffect
}
