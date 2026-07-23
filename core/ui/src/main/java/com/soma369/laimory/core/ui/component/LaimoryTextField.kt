package com.soma369.laimory.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * Figma TextField의 Default / Focused / Error / Disabled 상태를 공통으로 표현한다.
 *
 * 포커스 상태는 [interactionSource]에서 자동으로 파생하며, 오류와 비활성 상태가 포커스보다 우선한다.
 */
@Composable
fun LaimoryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supportingText: String? = null,
    counterText: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    fieldHeight: Dp = TextFieldHeight,
    focusRequester: FocusRequester? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaimoryTextFieldLayout(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        counterText = counterText,
        error = error,
        enabled = enabled,
        singleLine = singleLine,
        fieldHeight = fieldHeight,
        isFocused = isFocused,
        interactionSource = interactionSource,
        focusRequester = focusRequester,
        modifier = modifier,
    )
}

@Composable
private fun LaimoryTextFieldLayout(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    supportingText: String?,
    counterText: String?,
    error: String?,
    enabled: Boolean,
    singleLine: Boolean,
    fieldHeight: Dp,
    isFocused: Boolean,
    interactionSource: MutableInteractionSource,
    focusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    val isError = error != null
    val visuals =
        laimoryFieldVisuals(
            enabled = enabled,
            isActive = isFocused,
            isError = isError,
        )

    Column(
        modifier = modifier.alpha(visuals.alpha),
        verticalArrangement = Arrangement.spacedBy(TextFieldContentSpacing),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = visuals.labelColor,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .then(
                        if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
                    )
                    .fillMaxWidth()
                    .height(fieldHeight)
                    .background(visuals.containerColor, MaterialTheme.shapes.medium)
                    .border(
                        border =
                            BorderStroke(
                                width = visuals.borderWidth,
                                color = visuals.borderColor,
                            ),
                        shape = MaterialTheme.shapes.medium,
                    ),
            enabled = enabled,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyMedium.merge(TextStyle(color = visuals.contentColor)),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.large),
                    contentAlignment =
                        if (singleLine) {
                            Alignment.CenterStart
                        } else {
                            Alignment.TopStart
                        },
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            modifier = if (singleLine) Modifier else Modifier.padding(vertical = Spacing.large),
                            style = MaterialTheme.typography.bodyMedium,
                            color = visuals.contentColor,
                        )
                    }
                    Box(
                        modifier = if (singleLine) Modifier else Modifier.padding(vertical = Spacing.large),
                    ) {
                        innerTextField()
                    }
                }
            },
        )
        if (error != null || supportingText != null || counterText != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = error ?: supportingText.orEmpty(),
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelMedium,
                    color = visuals.supportingColor,
                )
                counterText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(name = "TextField 상태", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryTextFieldStatesPreview() {
    LaimoryTheme {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
        ) {
            TextFieldPreviewItem(state = PreviewState.Default)
            TextFieldPreviewItem(state = PreviewState.Focused)
            TextFieldPreviewItem(state = PreviewState.Error)
            TextFieldPreviewItem(state = PreviewState.Disabled)
        }
    }
}

@Preview(name = "TextField 다크", showBackground = true, widthDp = 360)
@Composable
private fun LaimoryTextFieldDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(Spacing.large)) {
            TextFieldPreviewItem(state = PreviewState.Focused)
        }
    }
}

@Composable
private fun TextFieldPreviewItem(state: PreviewState) {
    LaimoryTextFieldLayout(
        value = if (state == PreviewState.Focused) "오늘의 기록" else "",
        onValueChange = {},
        label = "라벨",
        placeholder = "내용을 입력하세요",
        supportingText = "도움말 텍스트",
        counterText = null,
        error = if (state == PreviewState.Error) "필수 입력 항목이에요" else null,
        enabled = state != PreviewState.Disabled,
        singleLine = true,
        fieldHeight = TextFieldHeight,
        isFocused = state == PreviewState.Focused,
        interactionSource = remember { MutableInteractionSource() },
        focusRequester = null,
    )
}

private enum class PreviewState {
    Default,
    Focused,
    Error,
    Disabled,
}

private val TextFieldContentSpacing = 6.dp
private val TextFieldHeight = LaimoryFieldDefaultHeight
