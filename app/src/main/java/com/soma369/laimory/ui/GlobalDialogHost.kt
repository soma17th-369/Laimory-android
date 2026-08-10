package com.soma369.laimory.ui

import androidx.compose.runtime.Composable
import com.soma369.laimory.core.domain.message.ActiveDialog
import com.soma369.laimory.core.domain.message.DialogActionStyle
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogActionStyle
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons

/**
 * Root가 렌더링하는 공통 메시지형 Dialog 호스트.
 *
 * 활성 [ActiveDialog]를 `LaimoryDialog`로 매핑만 하고, 사용자 선택은 requestId와 함께
 * [onResult]로 반환한다 — Repository/UseCase 호출이나 화면 상태 전이는 수행하지 않는다.
 */
@Composable
internal fun GlobalDialogHost(
    activeDialog: ActiveDialog?,
    onResult: (requestId: Long, result: DialogResult) -> Unit,
) {
    val dialog = activeDialog ?: return
    LaimoryDialog(
        title = dialog.request.title,
        body = dialog.request.body,
        buttons = dialog.toButtons(onResult),
        onDismissRequest = { onResult(dialog.requestId, DialogResult.Dismissed) },
        dismissible = dialog.request.dismissible,
    )
}

private fun ActiveDialog.toButtons(onResult: (Long, DialogResult) -> Unit): LaimoryDialogButtons =
    when (val request = request) {
        is DialogRequest.OneButton ->
            LaimoryDialogButtons.One(
                label = request.buttonLabel,
                onClick = { onResult(requestId, DialogResult.Primary) },
                style = request.buttonStyle.toUiStyle(),
            )

        is DialogRequest.TwoButton ->
            LaimoryDialogButtons.Two(
                secondaryLabel = request.secondaryLabel,
                onSecondaryClick = { onResult(requestId, DialogResult.Secondary) },
                primaryLabel = request.primaryLabel,
                onPrimaryClick = { onResult(requestId, DialogResult.Primary) },
                primaryStyle = request.primaryStyle.toUiStyle(),
            )
    }

private fun DialogActionStyle.toUiStyle(): LaimoryDialogActionStyle =
    when (this) {
        DialogActionStyle.PRIMARY -> LaimoryDialogActionStyle.Primary
        DialogActionStyle.DESTRUCTIVE -> LaimoryDialogActionStyle.Destructive
    }
