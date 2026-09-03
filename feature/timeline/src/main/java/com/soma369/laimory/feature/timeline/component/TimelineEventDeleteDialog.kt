package com.soma369.laimory.feature.timeline.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogActionStyle
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.feature.timeline.state.TimelineEventDeleteDialogState

/**
 * 이벤트 삭제 확인.
 *
 * 카드의 휴지통은 한 번 누르면 되돌릴 수 없는 자리라, 사진 삭제와 같은 형태로 한 번 더 묻는다.
 * 성공 화면은 두지 않는다 — 지워진 이벤트가 목록에서 사라지는 것이 곧 결과다.
 */
@Composable
fun TimelineEventDeleteDialog(
    state: TimelineEventDeleteDialogState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (state == TimelineEventDeleteDialogState.Hidden) return

    LaimoryDialog(
        onDismissRequest = {
            if (state !is TimelineEventDeleteDialogState.Deleting) onDismiss()
        },
        title =
            when (state) {
                is TimelineEventDeleteDialogState.Confirmation -> "이 이벤트를 삭제할까요?"
                is TimelineEventDeleteDialogState.Deleting -> "이벤트를 삭제하고 있어요"
                is TimelineEventDeleteDialogState.RetryableError -> "이벤트를 삭제하지 못했어요"
                TimelineEventDeleteDialogState.Hidden -> ""
            },
        body =
            when (state) {
                is TimelineEventDeleteDialogState.Confirmation ->
                    "이 이벤트와 여기에 연결된 사진이 함께 삭제되며 되돌릴 수 없어요.\n" +
                        "기기의 원본 사진은 삭제되지 않습니다."
                is TimelineEventDeleteDialogState.Deleting -> "잠시만 기다려주세요."
                is TimelineEventDeleteDialogState.RetryableError ->
                    "${state.message}\n다른 이벤트와 편집 중인 내용은 그대로 유지됩니다."
                TimelineEventDeleteDialogState.Hidden -> ""
            },
        buttons =
            when (state) {
                is TimelineEventDeleteDialogState.Confirmation ->
                    LaimoryDialogButtons.Two(
                        secondaryLabel = "취소",
                        onSecondaryClick = onDismiss,
                        primaryLabel = "삭제",
                        onPrimaryClick = onConfirm,
                        primaryStyle = LaimoryDialogActionStyle.Destructive,
                    )
                is TimelineEventDeleteDialogState.Deleting ->
                    LaimoryDialogButtons.One(
                        label = "삭제 중",
                        onClick = {},
                        isLoading = true,
                    )
                is TimelineEventDeleteDialogState.RetryableError ->
                    LaimoryDialogButtons.Two(
                        secondaryLabel = "취소",
                        onSecondaryClick = onDismiss,
                        primaryLabel = "다시 시도",
                        onPrimaryClick = onConfirm,
                        primaryStyle = LaimoryDialogActionStyle.Destructive,
                    )
                TimelineEventDeleteDialogState.Hidden -> return
            },
        dismissible = state !is TimelineEventDeleteDialogState.Deleting,
    )
}

@Preview(showBackground = true)
@Composable
private fun TimelineEventDeleteConfirmationPreview() {
    LaimoryTheme {
        TimelineEventDeleteDialog(
            state = TimelineEventDeleteDialogState.Confirmation(timelineEventId = 1L),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineEventDeletingPreview() {
    LaimoryTheme {
        TimelineEventDeleteDialog(
            state = TimelineEventDeleteDialogState.Deleting(timelineEventId = 1L),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineEventDeleteFailurePreview() {
    LaimoryTheme {
        TimelineEventDeleteDialog(
            state =
                TimelineEventDeleteDialogState.RetryableError(
                    timelineEventId = 1L,
                    message = "네트워크 상태를 확인한 뒤 다시 시도해주세요.",
                ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
