package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.domain.model.notice.Notice
import com.soma369.laimory.core.ui.base.UiIntent

sealed interface NoticesUiIntent : UiIntent {
    /**
     * 진입·복귀(ON_RESUME) 시 목록을 다시 받는다.
     *
     * 캐시하지 않는다 — 목록이 작고, 관리자가 숨긴 공지가 남아 있으면 안 된다. 이미 목록을
     * 보여 주는 중이면 그대로 둔 채 갱신한다(깜빡임 방지).
     */
    data object Sync : NoticesUiIntent

    data class NoticeClicked(
        val notice: Notice,
    ) : NoticesUiIntent

    /** 원문이 실제로 열렸다. 이때 읽음으로 남긴다. */
    data class NoticeOpened(
        val notice: Notice,
    ) : NoticesUiIntent

    data object NavigateBack : NoticesUiIntent
}
