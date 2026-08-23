package com.soma369.laimory.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.soma369.laimory.core.domain.message.ActiveDialog
import com.soma369.laimory.core.domain.message.DialogActionStyle
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogActionStyle
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons
import com.soma369.laimory.core.ui.component.LaimoryDialogConsent

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
    // 확인 체크는 표시 중에만 쓰는 값이라 도메인 요청·결과에 담지 않고 호스트가 들고 있는다.
    // requestId 로 key 를 잡아 다음 Dialog 가 이전 체크 상태를 물려받지 않게 한다.
    var consented by remember(dialog.requestId) { mutableStateOf(false) }

    LaimoryDialog(
        title = dialog.request.title,
        body = dialog.request.body,
        buttons = dialog.toButtons(consented = consented, onResult = onResult),
        onDismissRequest = { onResult(dialog.requestId, DialogResult.Dismissed) },
        dismissible = dialog.request.dismissible,
        consent =
            (dialog.request as? DialogRequest.Consent)?.let { request ->
                LaimoryDialogConsent(
                    label = request.consentLabel,
                    checked = consented,
                    onCheckedChange = { consented = it },
                )
            },
    )
}

private fun ActiveDialog.toButtons(
    consented: Boolean,
    onResult: (Long, DialogResult) -> Unit,
): LaimoryDialogButtons =
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

        // 체크 전에는 primary 만 잠근다(취소는 열어 둔다). 그래서 Primary 결과 자체가 동의를
        // 뜻하고, 호출부는 체크 여부를 따로 확인하지 않는다.
        is DialogRequest.Consent ->
            LaimoryDialogButtons.Two(
                secondaryLabel = request.secondaryLabel,
                onSecondaryClick = { onResult(requestId, DialogResult.Secondary) },
                primaryLabel = request.primaryLabel,
                onPrimaryClick = { onResult(requestId, DialogResult.Primary) },
                primaryStyle = request.primaryStyle.toUiStyle(),
                primaryEnabled = consented,
            )
    }

private fun DialogActionStyle.toUiStyle(): LaimoryDialogActionStyle =
    when (this) {
        DialogActionStyle.PRIMARY -> LaimoryDialogActionStyle.Primary
        DialogActionStyle.DESTRUCTIVE -> LaimoryDialogActionStyle.Destructive
    }
