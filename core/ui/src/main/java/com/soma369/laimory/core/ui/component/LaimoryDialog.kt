package com.soma369.laimory.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
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
) {
    Dialog(
        onDismissRequest = {
            if (dismissible) onDismissRequest()
        },
        properties =
            DialogProperties(
                dismissOnBackPress = dismissible,
                dismissOnClickOutside = dismissible,
            ),
    ) {
        Surface(
            modifier = modifier.width(DialogWidth),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = DialogShadowElevation,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = Spacing.extraLarge2)
                        .padding(top = DialogTopPadding),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(Spacing.large))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.large))
                DialogActions(buttons = buttons)
            }
        }
    }
}

@Composable
private fun DialogActions(buttons: LaimoryDialogButtons) {
    Column {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(DialogDividerHeight)
                    .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(DialogActionHeight),
        ) {
            when (buttons) {
                is LaimoryDialogButtons.One ->
                    DialogActionButton(
                        label = buttons.label,
                        onClick = buttons.onClick,
                        style = buttons.style,
                        enabled = buttons.enabled,
                        isLoading = buttons.isLoading,
                        modifier = Modifier.weight(1f),
                    )

                is LaimoryDialogButtons.Two -> {
                    DialogActionButton(
                        label = buttons.secondaryLabel,
                        onClick = buttons.onSecondaryClick,
                        style = LaimoryDialogActionStyle.Neutral,
                        enabled = buttons.enabled,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier =
                            Modifier
                                .width(DialogActionDividerWidth)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    DialogActionButton(
                        label = buttons.primaryLabel,
                        onClick = buttons.onPrimaryClick,
                        style = buttons.primaryStyle,
                        enabled = buttons.enabled,
                        isLoading = buttons.isPrimaryLoading,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogActionButton(
    label: String,
    onClick: () -> Unit,
    style: LaimoryDialogActionStyle,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val contentColor =
        when (style) {
            LaimoryDialogActionStyle.Primary -> MaterialTheme.colorScheme.primary
            LaimoryDialogActionStyle.Destructive -> MaterialTheme.colorScheme.error
            LaimoryDialogActionStyle.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val actionEnabled = enabled && !isLoading

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .clickable(
                    enabled = actionEnabled,
                    role = Role.Button,
                    onClick = onClick,
                ).alpha(if (enabled) ENABLED_ALPHA else DISABLED_ALPHA),
        contentAlignment = Alignment.Center,
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
                color = contentColor,
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
        val primaryStyle: LaimoryDialogActionStyle = LaimoryDialogActionStyle.Destructive,
        val enabled: Boolean = true,
        val isPrimaryLoading: Boolean = false,
    ) : LaimoryDialogButtons
}

enum class LaimoryDialogActionStyle {
    Primary,
    Destructive,
    Neutral,
}

@Preview(name = "Dialog / Two", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryTwoButtonDialogPreview() {
    LaimoryTheme {
        LaimoryDialog(
            title = "이벤트 삭제",
            body = "이 이벤트를 삭제하시겠습니까?\n삭제된 이벤트는 복구할 수 없습니다.",
            buttons =
                LaimoryDialogButtons.Two(
                    secondaryLabel = "취소",
                    onSecondaryClick = {},
                    primaryLabel = "삭제",
                    onPrimaryClick = {},
                ),
            onDismissRequest = {},
        )
    }
}

@Preview(name = "Dialog / One", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryOneButtonDialogPreview() {
    LaimoryTheme {
        LaimoryDialog(
            title = "이벤트 삭제",
            body = "이 이벤트를 삭제하시겠습니까?\n삭제된 이벤트는 복구할 수 없습니다.",
            buttons =
                LaimoryDialogButtons.One(
                    label = "확인",
                    onClick = {},
                ),
            onDismissRequest = {},
        )
    }
}

@Preview(name = "Dialog / Two / Dark", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryTwoButtonDialogDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        LaimoryDialog(
            title = "이벤트 삭제",
            body = "이 이벤트를 삭제하시겠습니까?\n삭제된 이벤트는 복구할 수 없습니다.",
            buttons =
                LaimoryDialogButtons.Two(
                    secondaryLabel = "취소",
                    onSecondaryClick = {},
                    primaryLabel = "삭제",
                    onPrimaryClick = {},
                ),
            onDismissRequest = {},
        )
    }
}

@Preview(name = "Dialog / One / Dark", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryOneButtonDialogDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        LaimoryDialog(
            title = "이벤트 삭제",
            body = "이 이벤트를 삭제하시겠습니까?\n삭제된 이벤트는 복구할 수 없습니다.",
            buttons =
                LaimoryDialogButtons.One(
                    label = "확인",
                    onClick = {},
                ),
            onDismissRequest = {},
        )
    }
}

private val DialogWidth = 280.dp
private val DialogTopPadding = 28.dp
private val DialogActionHeight = 48.dp
private val DialogDividerHeight = 1.dp
private val DialogActionDividerWidth = 0.5.dp
private val DialogShadowElevation = 8.dp
private val DialogProgressSize = 20.dp
private val DialogProgressStrokeWidth = 2.dp
private const val ENABLED_ALPHA = 1f
private const val DISABLED_ALPHA = 0.38f
