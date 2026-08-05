package com.soma369.laimory.feature.timeline.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogActionStyle
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.feature.timeline.state.TimelineEventExistingPhoto
import com.soma369.laimory.feature.timeline.state.TimelineEventPhotoDeleteDialogState

@Composable
fun TimelineEventPhotoDeleteDialog(
    state: TimelineEventPhotoDeleteDialogState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (state == TimelineEventPhotoDeleteDialogState.Hidden) return

    LaimoryDialog(
        onDismissRequest = {
            if (state !is TimelineEventPhotoDeleteDialogState.Deleting) onDismiss()
        },
        title =
            when (state) {
                is TimelineEventPhotoDeleteDialogState.Confirmation -> "이 사진을 제거할까요?"
                is TimelineEventPhotoDeleteDialogState.Deleting -> "사진을 제거하고 있어요"
                is TimelineEventPhotoDeleteDialogState.RetryableError -> "사진을 제거하지 못했어요"
                TimelineEventPhotoDeleteDialogState.Hidden -> ""
            },
        body =
            when (state) {
                is TimelineEventPhotoDeleteDialogState.Confirmation ->
                    "현재 이벤트에서 바로 제거되며 되돌릴 수 없어요.\n기기의 원본 사진은 삭제되지 않습니다."
                is TimelineEventPhotoDeleteDialogState.Deleting -> "잠시만 기다려주세요."
                is TimelineEventPhotoDeleteDialogState.RetryableError ->
                    "${state.message}\n사진과 편집 중인 내용은 그대로 유지됩니다."
                TimelineEventPhotoDeleteDialogState.Hidden -> ""
            },
        buttons =
            when (state) {
                is TimelineEventPhotoDeleteDialogState.Confirmation ->
                    LaimoryDialogButtons.Two(
                        secondaryLabel = "취소",
                        onSecondaryClick = onDismiss,
                        primaryLabel = "제거",
                        onPrimaryClick = onConfirm,
                        primaryStyle = LaimoryDialogActionStyle.Destructive,
                    )
                is TimelineEventPhotoDeleteDialogState.Deleting ->
                    LaimoryDialogButtons.One(
                        label = "제거 중",
                        onClick = {},
                        isLoading = true,
                    )
                is TimelineEventPhotoDeleteDialogState.RetryableError ->
                    LaimoryDialogButtons.Two(
                        secondaryLabel = "취소",
                        onSecondaryClick = onDismiss,
                        primaryLabel = "다시 시도",
                        onPrimaryClick = onConfirm,
                        primaryStyle = LaimoryDialogActionStyle.Destructive,
                    )
                TimelineEventPhotoDeleteDialogState.Hidden -> return
            },
        dismissible = state !is TimelineEventPhotoDeleteDialogState.Deleting,
    )
}

@Preview(showBackground = true)
@Composable
private fun TimelineEventPhotoDeleteConfirmationPreview() {
    LaimoryTheme {
        TimelineEventPhotoDeleteDialog(
            state = TimelineEventPhotoDeleteDialogState.Confirmation(PreviewPhoto),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineEventPhotoDeletingPreview() {
    LaimoryTheme {
        TimelineEventPhotoDeleteDialog(
            state = TimelineEventPhotoDeleteDialogState.Deleting(PreviewPhoto),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineEventPhotoDeleteFailurePreview() {
    LaimoryTheme {
        TimelineEventPhotoDeleteDialog(
            state =
                TimelineEventPhotoDeleteDialogState.RetryableError(
                    photo = PreviewPhoto,
                    message = "네트워크 상태를 확인한 뒤 다시 시도해주세요.",
                ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

private val PreviewPhoto = TimelineEventExistingPhoto(timelineItemId = 1L, photoUrl = null)
