package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing
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

    val isDeleting = state == TimelineDeleteDialogState.Deleting
    AlertDialog(
        onDismissRequest = {
            when (state) {
                TimelineDeleteDialogState.Success -> onFinish()
                TimelineDeleteDialogState.Deleting -> Unit
                else -> onDismiss()
            }
        },
        title = {
            Text(
                text =
                    when (state) {
                        TimelineDeleteDialogState.Confirmation -> confirmationTitle
                        TimelineDeleteDialogState.Deleting -> "삭제하고 있어요"
                        is TimelineDeleteDialogState.RetryableError -> "삭제하지 못했어요"
                        TimelineDeleteDialogState.Success -> "삭제했어요"
                        TimelineDeleteDialogState.Hidden -> ""
                    },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Text(
                    text =
                        when (state) {
                            TimelineDeleteDialogState.Confirmation -> confirmationMessage
                            TimelineDeleteDialogState.Deleting -> "잠시만 기다려주세요."
                            is TimelineDeleteDialogState.RetryableError -> state.message
                            TimelineDeleteDialogState.Success -> successMessage
                            TimelineDeleteDialogState.Hidden -> ""
                        },
                )
                if (state is TimelineDeleteDialogState.RetryableError) {
                    Text(
                        text = "기존 기록은 그대로 유지됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            when (state) {
                TimelineDeleteDialogState.Confirmation ->
                    TextButton(
                        onClick = onConfirm,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("삭제")
                    }
                TimelineDeleteDialogState.Deleting ->
                    CircularProgressIndicator()
                is TimelineDeleteDialogState.RetryableError ->
                    TextButton(onClick = onConfirm) {
                        Text("다시 시도")
                    }
                TimelineDeleteDialogState.Success ->
                    TextButton(onClick = onFinish) {
                        Text("확인")
                    }
                TimelineDeleteDialogState.Hidden -> Unit
            }
        },
        dismissButton = {
            if (!isDeleting && state != TimelineDeleteDialogState.Success) {
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        },
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
