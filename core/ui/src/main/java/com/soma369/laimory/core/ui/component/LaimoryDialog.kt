package com.soma369.laimory.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * Laimory 디자인 시스템의 One / Two button 다이얼로그.
 *
 * 버튼 구성은 [LaimoryDialogButtons]로 제한해 Figma variant와 동일한 레이아웃을 유지한다.
 * 위험 동작은 [LaimoryDialogActionStyle.Destructive]를 명시해 확인 액션에만 error 색상을 적용한다.
 */
@Composable
fun LaimoryDialog(
    title: String,
    body: String,
    buttons: LaimoryDialogButtons,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    consent: LaimoryDialogConsent? = null,
) {
    Dialog(
        onDismissRequest = {
            if (dismissible) onDismissRequest()
        },
        properties =
            DialogProperties(
                dismissOnBackPress = dismissible,
                dismissOnClickOutside = dismissible,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(Spacing.extraLarge2),
            contentAlignment = Alignment.Center,
        ) {
            LaimoryDialogContent(
                title = title,
                body = body,
                buttons = buttons,
                modifier = modifier,
                consent = consent,
            )
        }
    }
}

@Composable
private fun LaimoryDialogContent(
    title: String,
    body: String,
    buttons: LaimoryDialogButtons,
    modifier: Modifier = Modifier,
    consent: LaimoryDialogConsent? = null,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .widthIn(max = DialogMaxWidth),
        shape = RoundedCornerShape(DialogCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = DialogShadowElevation,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    start = Spacing.extraLarge2,
                    top = Spacing.extraLarge2,
                    end = Spacing.extraLarge2,
                    bottom = Spacing.extraLarge,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                modifier = Modifier.heightIn(min = DialogBodyMinHeight),
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (consent != null) DialogConsentRow(consent = consent)
            DialogActions(buttons = buttons)
        }
    }
}

@Composable
private fun LaimoryDialogPreviewContent(buttons: LaimoryDialogButtons) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(Spacing.extraLarge2),
        contentAlignment = Alignment.Center,
    ) {
        LaimoryDialogContent(
            title = "이벤트 삭제",
            body = "이 이벤트를 삭제하시겠습니까?\n삭제된 이벤트는 복구할 수 없습니다.",
            buttons = buttons,
        )
    }
}

@Composable
private fun LaimoryTwoButtonDialogPreviewContent() {
    LaimoryDialogPreviewContent(
        buttons =
            LaimoryDialogButtons.Two(
                secondaryLabel = "취소",
                onSecondaryClick = {},
                primaryLabel = "삭제",
                onPrimaryClick = {},
                primaryStyle = LaimoryDialogActionStyle.Destructive,
            ),
    )
}

@Composable
private fun LaimoryOneButtonDialogPreviewContent() {
    LaimoryDialogPreviewContent(
        buttons =
            LaimoryDialogButtons.One(
                label = "확인",
                onClick = {},
            ),
    )
}

@Composable
private fun LaimoryConsentDialogPreviewContent(checked: Boolean) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(Spacing.extraLarge2),
        contentAlignment = Alignment.Center,
    ) {
        LaimoryDialogContent(
            title = "계정을 삭제할까요?",
            body = "계정과 서버에 저장된 기록이 모두 삭제되며 되돌릴 수 없습니다.",
            consent =
                LaimoryDialogConsent(
                    label = "위 내용을 확인했으며 계정 삭제에 동의합니다",
                    checked = checked,
                    onCheckedChange = {},
                ),
            buttons =
                LaimoryDialogButtons.Two(
                    secondaryLabel = "취소",
                    onSecondaryClick = {},
                    primaryLabel = "계정 삭제",
                    onPrimaryClick = {},
                    primaryStyle = LaimoryDialogActionStyle.Destructive,
                    primaryEnabled = checked,
                ),
        )
    }
}

@Preview(name = "Dialog / Consent / Unchecked", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryConsentDialogPreview() {
    LaimoryTheme {
        LaimoryConsentDialogPreviewContent(checked = false)
    }
}

@Preview(name = "Dialog / Consent / Checked / Dark", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryConsentDialogCheckedDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        LaimoryConsentDialogPreviewContent(checked = true)
    }
}

@Preview(name = "Dialog / Two", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryTwoButtonDialogPreview() {
    LaimoryTheme {
        LaimoryTwoButtonDialogPreviewContent()
    }
}

@Preview(name = "Dialog / One", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryOneButtonDialogPreview() {
    LaimoryTheme {
        LaimoryOneButtonDialogPreviewContent()
    }
}

@Preview(name = "Dialog / Two / Dark", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryTwoButtonDialogDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        LaimoryTwoButtonDialogPreviewContent()
    }
}

@Preview(name = "Dialog / One / Dark", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryOneButtonDialogDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        LaimoryOneButtonDialogPreviewContent()
    }
}

/**
 * 확인 체크박스 행.
 *
 * 행 전체를 하나의 토글로 노출한다 — 체크박스만 누를 수 있으면 표적이 작고, 라벨과 체크박스가
 * 각각 읽히면 스크린 리더가 같은 내용을 두 번 말한다. 그래서 [Role.Checkbox] 를 행에 두고
 * 체크박스 자체의 클릭은 비운다.
 */
@Composable
private fun DialogConsentRow(consent: LaimoryDialogConsent) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .toggleable(
                    value = consent.checked,
                    enabled = consent.enabled,
                    role = Role.Checkbox,
                    onValueChange = consent.onCheckedChange,
                ).padding(vertical = Spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Checkbox(
            checked = consent.checked,
            onCheckedChange = null,
            enabled = consent.enabled,
        )
        Text(
            text = consent.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** [LaimoryDialog] 의 확인 체크박스. 체크 상태는 호출자가 소유한다. */
@Immutable
data class LaimoryDialogConsent(
    val label: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val enabled: Boolean = true,
)

@Composable
private fun DialogActions(buttons: LaimoryDialogButtons) {
    when (buttons) {
        is LaimoryDialogButtons.One ->
            DialogPrimaryButton(
                label = buttons.label,
                onClick = buttons.onClick,
                style = buttons.style,
                enabled = buttons.enabled,
                isLoading = buttons.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

        is LaimoryDialogButtons.Two ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                DialogSecondaryButton(
                    label = buttons.secondaryLabel,
                    onClick = buttons.onSecondaryClick,
                    enabled = buttons.enabled,
                    modifier = Modifier.weight(1f),
                )
                DialogPrimaryButton(
                    label = buttons.primaryLabel,
                    onClick = buttons.onPrimaryClick,
                    style = buttons.primaryStyle,
                    enabled = buttons.enabled && buttons.primaryEnabled,
                    isLoading = buttons.isPrimaryLoading,
                    modifier = Modifier.weight(1f),
                )
            }
    }
}

@Composable
private fun DialogSecondaryButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        modifier = modifier.height(DialogActionHeight),
        enabled = enabled,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        contentPadding = DialogActionContentPadding,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun DialogPrimaryButton(
    label: String,
    onClick: () -> Unit,
    style: LaimoryDialogActionStyle,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val containerColor =
        when (style) {
            LaimoryDialogActionStyle.Primary -> MaterialTheme.colorScheme.primary
            LaimoryDialogActionStyle.Destructive -> MaterialTheme.colorScheme.error
        }
    val contentColor =
        when (style) {
            LaimoryDialogActionStyle.Primary -> MaterialTheme.colorScheme.onPrimary
            LaimoryDialogActionStyle.Destructive -> MaterialTheme.colorScheme.onError
        }
    val actionEnabled = enabled && !isLoading

    Button(
        modifier = modifier.height(DialogActionHeight),
        enabled = actionEnabled,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        contentPadding = DialogActionContentPadding,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(DialogProgressSize),
                color = contentColor,
                strokeWidth = DialogProgressStrokeWidth,
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Immutable
sealed interface LaimoryDialogButtons {
    data class One(
        val label: String,
        val onClick: () -> Unit,
        val style: LaimoryDialogActionStyle = LaimoryDialogActionStyle.Primary,
        val enabled: Boolean = true,
        val isLoading: Boolean = false,
    ) : LaimoryDialogButtons

    data class Two(
        val secondaryLabel: String,
        val onSecondaryClick: () -> Unit,
        val primaryLabel: String,
        val onPrimaryClick: () -> Unit,
        val primaryStyle: LaimoryDialogActionStyle = LaimoryDialogActionStyle.Primary,
        val enabled: Boolean = true,
        val isPrimaryLoading: Boolean = false,
        /** primary 만 따로 잠근다. 확인 체크박스처럼 취소는 열어 두고 확인만 막을 때 쓴다. */
        val primaryEnabled: Boolean = true,
    ) : LaimoryDialogButtons
}

enum class LaimoryDialogActionStyle {
    Primary,
    Destructive,
}

private val DialogMaxWidth = 320.dp
private val DialogBodyMinHeight = 48.dp
private val DialogCornerRadius = 24.dp
private val DialogActionHeight = 48.dp
private val DialogShadowElevation = 8.dp
private val DialogProgressSize = 16.dp
private val DialogProgressStrokeWidth = 2.dp
private val DialogActionContentPadding =
    PaddingValues(
        horizontal = Spacing.large,
        vertical = Spacing.medium,
    )
