package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable

/**
 * 이벤트 삭제 확인 다이얼로그 상태.
 *
 * 하루 기록 삭제([TimelineDeleteDialogState])와 나눠 둔다 — 지우는 대상이 다르고, 기록 삭제는
 * 성공하면 화면을 떠나지만 이벤트 삭제는 목록에 남는다. 한 상태를 돌려 쓰면 어느 쪽 요청인지
 * 알 수 없어 성공 처리가 갈린다.
 *
 * 대상 Event 는 상태가 직접 들고 있다. 다시 시도할 때 열 때 정한 대상이 그대로 따라가야 한다 —
 * 목록은 그사이에도 갱신되므로 화면에서 다시 찾으면 다른 이벤트를 지울 수 있다.
 */
@Immutable
sealed interface TimelineEventDeleteDialogState {
    data object Hidden : TimelineEventDeleteDialogState

    /** 지울 대상이 정해진 상태. */
    sealed interface Active : TimelineEventDeleteDialogState {
        val timelineEventId: Long
    }

    data class Confirmation(
        override val timelineEventId: Long,
    ) : Active

    data class Deleting(
        override val timelineEventId: Long,
    ) : Active

    data class RetryableError(
        override val timelineEventId: Long,
        val message: String,
    ) : Active
}
