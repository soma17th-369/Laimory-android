package com.soma369.laimory.feature.timeline.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogActionStyle
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.feature.timeline.state.TimelineDeleteDialogState

@Composable
fun TimelineDeleteDialog(
    state: TimelineDeleteDialogState,
    confirmationTitle: String,
    confirmationMessage: String,
    successMessage: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onFinish: () -> Unit,
) {
    if (state == TimelineDeleteDialogState.Hidden) return

    LaimoryDialog(
        onDismissRequest = {
            when (state) {
                TimelineDeleteDialogState.Success -> onFinish()
                TimelineDeleteDialogState.Deleting -> Unit
                else -> onDismiss()
            }
        },
        title =
            when (state) {
                TimelineDeleteDialogState.Confirmation -> confirmationTitle
                TimelineDeleteDialogState.Deleting -> "삭제하고 있어요"
                is TimelineDeleteDialogState.RetryableError -> "삭제하지 못했어요"
                TimelineDeleteDialogState.Success -> "삭제했어요"
                TimelineDeleteDialogState.Hidden -> ""
            },
        body =
            when (state) {
                TimelineDeleteDialogState.Confirmation -> confirmationMessage
                TimelineDeleteDialogState.Deleting -> "잠시만 기다려주세요."
                is TimelineDeleteDialogState.RetryableError ->
                    "${state.message}\n기존 기록은 그대로 유지됩니다."
                TimelineDeleteDialogState.Success -> successMessage
                TimelineDeleteDialogState.Hidden -> ""
            },
        buttons =
            when (state) {
                TimelineDeleteDialogState.Confirmation ->
                    LaimoryDialogButtons.Two(
                        secondaryLabel = "취소",
                        onSecondaryClick = onDismiss,
                        primaryLabel = "삭제",
                        onPrimaryClick = onConfirm,
                    )
                TimelineDeleteDialogState.Deleting ->
                    LaimoryDialogButtons.One(
                        label = "삭제 중",
                        onClick = {},
                        isLoading = true,
                    )
                is TimelineDeleteDialogState.RetryableError ->
                    LaimoryDialogButtons.Two(
                        secondaryLabel = "취소",
                        onSecondaryClick = onDismiss,
                        primaryLabel = "다시 시도",
                        onPrimaryClick = onConfirm,
                        primaryStyle = LaimoryDialogActionStyle.Destructive,
                    )
                TimelineDeleteDialogState.Success ->
                    LaimoryDialogButtons.One(
                        label = "확인",
                        onClick = onFinish,
                    )
                TimelineDeleteDialogState.Hidden -> return
            },
        dismissible = state != TimelineDeleteDialogState.Deleting,
    )
}

@Preview(showBackground = true)
@Composable
private fun TimelineDeleteConfirmationPreview() {
    LaimoryTheme {
        TimelineDeleteDialog(
            state = TimelineDeleteDialogState.Confirmation,
            confirmationTitle = "이 이벤트를 삭제할까요?",
            confirmationMessage = "이벤트와 연결된 항목이 함께 삭제됩니다.",
            successMessage = "이벤트를 삭제했습니다.",
            onConfirm = {},
            onDismiss = {},
            onFinish = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineDeleteProgressPreview() {
    LaimoryTheme {
        TimelineDeleteDialog(
            state = TimelineDeleteDialogState.Deleting,
            confirmationTitle = "",
            confirmationMessage = "",
            successMessage = "",
            onConfirm = {},
            onDismiss = {},
            onFinish = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineDeleteFailurePreview() {
    LaimoryTheme {
        TimelineDeleteDialog(
            state = TimelineDeleteDialogState.RetryableError("사진을 삭제하지 못했습니다. 잠시 후 다시 시도해주세요."),
            confirmationTitle = "",
            confirmationMessage = "",
            successMessage = "",
            onConfirm = {},
            onDismiss = {},
            onFinish = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineDeleteSuccessPreview() {
    LaimoryTheme {
        TimelineDeleteDialog(
            state = TimelineDeleteDialogState.Success,
            confirmationTitle = "",
            confirmationMessage = "",
            successMessage = "이벤트를 삭제했습니다.",
            onConfirm = {},
            onDismiss = {},
            onFinish = {},
        )
    }
}
