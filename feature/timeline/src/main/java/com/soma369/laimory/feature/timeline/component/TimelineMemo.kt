package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soma369.laimory.core.domain.model.timeline.TimelineEventMemoPolicy
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimorySignature
import com.soma369.laimory.feature.timeline.model.DEFAULT_MEMO_PROMPT
import com.soma369.laimory.feature.timeline.model.TimelineMemoDisplay
import com.soma369.laimory.feature.timeline.model.timelineMemoDisplay
import com.soma369.laimory.feature.timeline.state.TimelineMemoEditorState

@Composable
internal fun TimelineMemo(
    memo: String?,
    question: String?,
    editor: TimelineMemoEditorState?,
    isEditable: Boolean,
    onClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (editor == null) {
        val display = timelineMemoDisplay(memo = memo, question = question, isEditable = isEditable) ?: return
        Text(
            text = display.text,
            modifier =
                Modifier
                    .fillMaxWidth()
                    // 점선 테두리와 안쪽 여백은 "여기를 눌러 적을 수 있다"는 표시라 편집 모드에서만 둔다.
                    // 읽기 모드에서는 메모가 본문의 한 문단이므로 테를 두르면 입력칸으로 오인된다.
                    .then(
                        if (isEditable) {
                            Modifier
                                .dashedRoundRectBorder(
                                    color = MaterialTheme.colorScheme.outline,
                                    cornerRadius = MaterialTheme.shapes.medium.topStart,
                                ).clickable(onClick = onClick)
                                .padding(horizontal = Spacing.medium, vertical = 10.dp)
                        } else {
                            Modifier
                        },
                    ),
            style = if (isEditable) MaterialTheme.laimorySignature.note else readModeMemoStyle(),
            color =
                if (display is TimelineMemoDisplay.Memo) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = if (display is TimelineMemoDisplay.Question) QUESTION_MAX_LINES else MEMO_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }

    TimelineMemoEditor(
        editor = editor,
        placeholder = question?.takeIf(String::isNotBlank) ?: DEFAULT_MEMO_PROMPT,
        onValueChange = onValueChange,
        onCancel = onCancel,
        onConfirm = onConfirm,
    )
}

private const val MEMO_MAX_LINES = 3

/** question 은 서버 기준 255자까지 온다. 기본 안내 문구보다 여유를 둔다. */
private const val QUESTION_MAX_LINES = 5

@Composable
private fun TimelineMemoEditor(
    editor: TimelineMemoEditorState,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val focusRequester = remember(editor.timelineEventId) { FocusRequester() }
    val editorBottomBringIntoViewRequester = remember(editor.timelineEventId) { BringIntoViewRequester() }
    var textFieldValue by remember(editor.timelineEventId) {
        mutableStateOf(
            TextFieldValue(
                text = editor.draftMemo,
                selection = TextRange(editor.draftMemo.length),
            ),
        )
    }
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val locale = LocalLocale.current.platformLocale
    val textColor = MaterialTheme.colorScheme.onBackground
    LaunchedEffect(editor.timelineEventId) {
        focusRequester.requestFocus()
        imeInsets.awaitSettled(density)
        withFrameNanos { }
        editorBottomBringIntoViewRequester.bringIntoView()
    }
    LaunchedEffect(editor.draftMemo) {
        if (editor.draftMemo != textFieldValue.text) {
            textFieldValue =
                TextFieldValue(
                    text = editor.draftMemo,
                    selection = TextRange(editor.draftMemo.length),
                )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .dashedRoundRectBorder(
                        color = MaterialTheme.colorScheme.primary,
                        cornerRadius = MaterialTheme.shapes.medium.topStart,
                        strokeWidth = 1.5.dp,
                    ).padding(horizontal = Spacing.medium, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    onValueChange(it.text)
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 20.dp, max = 160.dp)
                        .focusRequester(focusRequester),
                enabled = !editor.isSaving,
                textStyle = MaterialTheme.laimorySignature.note.copy(color = textColor),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 8,
                decorationBox = { innerTextField ->
                    Box {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.laimorySignature.note,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (!editor.isValid) {
                Text(
                    text =
                        "메모는 ${String.format(locale, "%,d", TimelineEventMemoPolicy.MAX_LENGTH)}자까지 " +
                            "입력할 수 있어요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimelineMemoActionButton(
                    contentDescription = "메모 편집 취소",
                    enabled = !editor.isSaving,
                    onClick = onCancel,
                ) {
                    Text(
                        text = "×",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                TimelineMemoActionButton(
                    contentDescription = "메모 편집 완료",
                    enabled = editor.isConfirmEnabled,
                    onClick = onConfirm,
                ) {
                    if (editor.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Spacing.small)
                    .bringIntoViewRequester(editorBottomBringIntoViewRequester),
        )
    }
}

private suspend fun WindowInsets.awaitSettled(density: Density) {
    var previousBottom = -1
    var stableFrameCount = 0
    repeat(MAX_IME_WAIT_FRAME_COUNT) {
        withFrameNanos { }
        val currentBottom = getBottom(density)
        stableFrameCount =
            if (currentBottom > 0 && currentBottom == previousBottom) {
                stableFrameCount + 1
            } else {
                0
            }
        previousBottom = currentBottom
        if (stableFrameCount >= STABLE_IME_FRAME_COUNT) return
    }
}

@Composable
private fun TimelineMemoActionButton(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(28.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.semantics { this.contentDescription = contentDescription },
        ) {
            content()
        }
    }
}

/**
 * 읽기 모드 메모 서체.
 *
 * 읽기 화면에서 메모는 본문의 한 문단이라 편집기 안의 note(13sp)보다 커야 읽힌다.
 * `laimorySignature.note` 를 직접 키우지 않는 이유는 그 토큰을 로그인 화면도 쓰기 때문이다 —
 * 여기서만 크기를 덮고 서체(고운 바탕)는 그대로 물려받는다.
 *
 * 행간은 시안값을 그대로 쓴다. 글자 크기의 1.1 배라 여러 줄로 접히면 빽빽해진다.
 */
@Composable
private fun readModeMemoStyle() =
    MaterialTheme.laimorySignature.note.copy(
        fontSize = READ_MODE_MEMO_FONT_SIZE,
        lineHeight = READ_MODE_MEMO_LINE_HEIGHT,
    )

private fun Modifier.dashedRoundRectBorder(
    color: Color,
    cornerRadius: androidx.compose.foundation.shape.CornerSize,
    strokeWidth: Dp = 1.dp,
) = drawWithCache {
    val strokeWidthPx = strokeWidth.toPx()
    val radius = cornerRadius.toPx(size, this)
    val dash = 4.dp.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
    onDrawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            style = Stroke(width = strokeWidthPx, pathEffect = pathEffect),
        )
    }
}

private val READ_MODE_MEMO_FONT_SIZE = 20.sp
private val READ_MODE_MEMO_LINE_HEIGHT = 22.sp

private const val STABLE_IME_FRAME_COUNT = 2
private const val MAX_IME_WAIT_FRAME_COUNT = 60
