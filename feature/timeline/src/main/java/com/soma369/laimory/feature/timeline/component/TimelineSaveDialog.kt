package com.soma369.laimory.feature.timeline.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.feature.timeline.state.TimelineSaveDialogState

@Composable
internal fun TimelineSaveDialog(
    state: TimelineSaveDialogState,
    confirmationTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (state == TimelineSaveDialogState.Hidden) return

    LaimoryDialog(
        onDismissRequest = {
            if (state != TimelineSaveDialogState.Saving) onDismiss()
        },
        title =
            when (state) {
                TimelineSaveDialogState.Confirmation -> confirmationTitle
                TimelineSaveDialogState.Saving -> "저장하고 있어요"
                is TimelineSaveDialogState.RetryableError -> "저장하지 못했어요"
                TimelineSaveDialogState.Hidden -> ""
            },
        body =
            when (state) {
                TimelineSaveDialogState.Confirmation ->
                    "작성 완료 후에는 이 날짜의 기록을 수정하거나 삭제할 수 없습니다."
                TimelineSaveDialogState.Saving -> "잠시만 기다려주세요."
                is TimelineSaveDialogState.RetryableError ->
                    "${state.message}\n현재 초안은 그대로 유지됩니다."
                TimelineSaveDialogState.Hidden -> ""
            },
        buttons =
            when (state) {
                TimelineSaveDialogState.Confirmation ->
                    LaimoryDialogButtons.Two(
                        secondaryLabel = "취소",
                        onSecondaryClick = onDismiss,
                        primaryLabel = "작성 완료",
                        onPrimaryClick = onConfirm,
                    )
                TimelineSaveDialogState.Saving ->
                    LaimoryDialogButtons.One(
                        label = "저장 중",
                        onClick = {},
                        isLoading = true,
                    )
                is TimelineSaveDialogState.RetryableError ->
                    LaimoryDialogButtons.Two(
                        secondaryLabel = "취소",
                        onSecondaryClick = onDismiss,
                        primaryLabel = "다시 시도",
                        onPrimaryClick = onConfirm,
                    )
                TimelineSaveDialogState.Hidden -> return
            },
        dismissible = state != TimelineSaveDialogState.Saving,
    )
}

@Preview(showBackground = true)
@Composable
private fun TimelineSaveConfirmationPreview() {
    LaimoryTheme {
        TimelineSaveDialog(
            state = TimelineSaveDialogState.Confirmation,
            confirmationTitle = "8월 12일 기록 작성을 완료할까요?",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineSaveProgressPreview() {
    LaimoryTheme {
        TimelineSaveDialog(
            state = TimelineSaveDialogState.Saving,
            confirmationTitle = "",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimelineSaveFailurePreview() {
    LaimoryTheme {
        TimelineSaveDialog(
            state = TimelineSaveDialogState.RetryableError("네트워크 상태를 확인한 뒤 다시 시도해주세요."),
            confirmationTitle = "",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
