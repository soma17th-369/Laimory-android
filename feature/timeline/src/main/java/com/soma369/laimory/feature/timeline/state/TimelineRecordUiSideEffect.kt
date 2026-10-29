package com.soma369.laimory.feature.timeline.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface TimelineRecordUiSideEffect : UiSideEffect {
    data class ShowSnackbar(
        val message: String,
    ) : TimelineRecordUiSideEffect

    /**
     * 메모 커밋이 실패했다. 되돌린 사실을 알리고 같은 값으로 다시 보낼 길을 준다.
     *
     * 실패한 값을 상태가 아니라 이 효과가 들고 간다 — 스낵바가 사라지면 재시도할 곳도 없으므로,
     * 화면 상태에 남겨 두면 언제 지울지가 다시 문제가 된다.
     */
    data class MemoCommitFailed(
        val timelineEventId: Long,
        val commitId: Long,
        val memo: String?,
        val message: String,
    ) : TimelineRecordUiSideEffect
}
