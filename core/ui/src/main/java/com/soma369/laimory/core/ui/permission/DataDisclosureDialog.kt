package com.soma369.laimory.core.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.soma369.laimory.core.ui.component.LaimoryDialog
import com.soma369.laimory.core.ui.component.LaimoryDialogButtons
import com.soma369.laimory.core.ui.theme.LaimoryTheme

/**
 * 권한 요청 직전의 수집 고지 창.
 *
 * `계속` 이 동의 버튼은 아니다 — 동의는 곧바로 뜨는 시스템 창이 받는다. 이 창은 그 전에 무엇을
 * 왜 읽는지 읽게 하는 자리라, 닫으면 요청하지 않고 끝낸다.
 */
@Composable
fun DataDisclosureDialog(
    disclosure: DataDisclosure,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaimoryDialog(
        title = disclosure.title,
        body = disclosure.body,
        buttons =
            LaimoryDialogButtons.Two(
                secondaryLabel = "취소",
                onSecondaryClick = onDismiss,
                primaryLabel = "계속",
                onPrimaryClick = onContinue,
            ),
        onDismissRequest = onDismiss,
    )
}

@Preview
@Composable
private fun LocationDisclosureDialogPreview() {
    LaimoryTheme {
        DataDisclosureDialog(disclosure = DataDisclosure.LOCATION, onContinue = {}, onDismiss = {})
    }
}

@Preview
@Composable
private fun PhotoDisclosureDialogPreview() {
    LaimoryTheme {
        DataDisclosureDialog(disclosure = DataDisclosure.PHOTO, onContinue = {}, onDismiss = {})
    }
}
