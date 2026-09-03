package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soma369.laimory.core.domain.model.timeline.TimelineEventMemoPolicy
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimorySignature
import com.soma369.laimory.feature.timeline.model.DEFAULT_MEMO_PROMPT
import com.soma369.laimory.feature.timeline.model.TimelineMemoDisplay
import com.soma369.laimory.feature.timeline.model.timelineMemoDisplay
import com.soma369.laimory.feature.timeline.state.TimelineMemoEditorState
import com.soma369.laimory.core.ui.R as UiR

/**
 * 이벤트 메모.
 *
 * 읽을 때나 쓸 때나 **왼쪽에 세로 획을 세운 인용**이다. 종전처럼 편집 모드에서만 점선 테두리를
 * 두르지 않는다 — 테두리는 입력칸의 표시라, 같은 문장이 모드에 따라 글이었다가 칸이 된다.
 * 편집 중인 동안만 획 색이 바뀌어 지금 손대는 자리를 알린다.
 */
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
                    .then(
                        // 읽기 모드의 메모는 본문의 한 문단이라 누를 곳이 없다.
                        if (isEditable) Modifier.clickable(onClick = onClick) else Modifier,
                    ).memoQuote(MaterialTheme.colorScheme.outline),
            style = memoQuoteStyle(),
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

    Column(
        modifier = Modifier.fillMaxWidth(),
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
                    .memoQuote(MaterialTheme.colorScheme.primaryContainer)
                    .heightIn(min = EDITOR_MIN_HEIGHT, max = EDITOR_MAX_HEIGHT)
                    .focusRequester(focusRequester),
            enabled = !editor.isSaving,
            textStyle = memoQuoteStyle().copy(color = textColor),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            maxLines = EDITOR_MAX_LINES,
            decorationBox = { innerTextField ->
                Box {
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = memoQuoteStyle(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
        // 앵커는 하단 바 자체다. 아래에 빈 Spacer 를 두면 그만큼 버튼 밑이 벌어지는데,
        // 시안의 편집 상태는 버튼 줄에서 끝난다.
        MemoEditorBottomBar(
            editor = editor,
            onCancel = onCancel,
            onConfirm = onConfirm,
            modifier = Modifier.bringIntoViewRequester(editorBottomBringIntoViewRequester),
        )
    }
}

/**
 * 편집 중 아래 줄 — 글자수와 취소·확인.
 *
 * 글자수는 상한을 넘겼을 때 색으로 알린다. 별도 안내 문장을 두지 않는 이유는 넘긴 사실과 얼마나
 * 넘겼는지를 같은 자리에서 이미 말하고 있기 때문이다.
 *
 * 왼쪽 여백은 획(2) + 간격(10) 만큼이다. 위의 메모 본문과 글자 시작선을 맞춘다.
 */
@Composable
private fun MemoEditorBottomBar(
    editor: TimelineMemoEditorState,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = QUOTE_RULE_WIDTH + QUOTE_RULE_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text =
                "${String.format(locale, "%,d", editor.draftMemo.length)}/" +
                    String.format(locale, "%,d", TimelineEventMemoPolicy.MAX_LENGTH),
            style = MaterialTheme.typography.labelSmall,
            color =
                if (editor.isValid) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            TimelineMemoActionButton(
                contentDescription = "메모 편집 취소",
                enabled = !editor.isSaving,
                onClick = onCancel,
            ) {
                Icon(
                    painter = painterResource(UiR.drawable.ico_default_close),
                    contentDescription = null,
                    modifier = Modifier.size(MEMO_ACTION_ICON_SIZE),
                )
            }
            TimelineMemoActionButton(
                contentDescription = "메모 편집 완료",
                enabled = editor.isConfirmEnabled,
                onClick = onConfirm,
            ) {
                if (editor.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(MEMO_ACTION_PROGRESS_SIZE),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Icon(
                        painter = painterResource(UiR.drawable.ico_default_check_filled),
                        contentDescription = null,
                        modifier = Modifier.size(MEMO_ACTION_ICON_SIZE),
                    )
                }
            }
        }
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
        modifier = Modifier.size(MEMO_ACTION_BUTTON_SIZE),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        // 시안은 primaryContainer 바탕 위에 본문 보조색 아이콘을 얹는다.
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
 * 인용 표시 — 왼쪽 세로 획과 그만큼의 안쪽 여백.
 *
 * 획을 자식으로 두고 높이를 맞추려면 `IntrinsicSize.Min` 이 필요한데, 편집 중에는 이 자리에
 * `BasicTextField` 가 들어와 intrinsic 측정을 기대할 수 없다. 그래서 배경으로 직접 그린다 —
 * 어떤 내용이 오든 그려진 높이가 곧 내용의 높이다.
 */
private fun Modifier.memoQuote(color: Color) =
    padding(top = QUOTE_TOP_PADDING)
        .drawBehind {
            drawRoundRect(
                color = color,
                size = Size(QUOTE_RULE_WIDTH.toPx(), size.height),
                cornerRadius = CornerRadius(QUOTE_RULE_RADIUS.toPx()),
            )
        }.padding(start = QUOTE_RULE_WIDTH + QUOTE_RULE_GAP)

/**
 * 메모 서체.
 *
 * 메모는 본문의 한 문단이라 편집기 안의 note(13sp)보다 커야 읽힌다. `laimorySignature.note` 를
 * 직접 키우지 않는 이유는 그 토큰을 로그인 화면도 쓰기 때문이다 — 여기서만 크기를 덮고
 * 서체(고운 바탕)는 그대로 물려받는다.
 *
 * 행간은 시안값을 그대로 쓴다. 글자 크기의 1.1 배라 여러 줄로 접히면 빽빽해진다.
 */
@Composable
private fun memoQuoteStyle() =
    MaterialTheme.laimorySignature.note.copy(
        fontSize = MEMO_FONT_SIZE,
        lineHeight = MEMO_LINE_HEIGHT,
    )

private val MEMO_FONT_SIZE = 20.sp
private val MEMO_LINE_HEIGHT = 22.sp

/** 인용 획. 시안 폭 2, 모서리 1, 본문과의 간격 10, 위 여백 2. */
private val QUOTE_RULE_WIDTH = 2.dp
private val QUOTE_RULE_RADIUS = 1.dp
private val QUOTE_RULE_GAP = 10.dp
private val QUOTE_TOP_PADDING = 2.dp

private val MEMO_ACTION_BUTTON_SIZE = 28.dp

/**
 * 아이콘이 버튼과 같은 크기다.
 *
 * 시안의 두 아이콘은 28 버튼 안에서 각각 18x18(X)·22x16(체크)을 차지한다. 에셋의 여백까지
 * 그 비율로 그려져 있어, 버튼 크기 그대로 두면 시안과 같은 글리프 크기가 나온다.
 */
private val MEMO_ACTION_ICON_SIZE = MEMO_ACTION_BUTTON_SIZE
private val MEMO_ACTION_PROGRESS_SIZE = 16.dp

/** 한 줄 높이. 빈 입력칸이 접히지 않게 잡아 둔다. */
private val EDITOR_MIN_HEIGHT = 22.dp
private val EDITOR_MAX_HEIGHT = 160.dp
private const val EDITOR_MAX_LINES = 8

private const val STABLE_IME_FRAME_COUNT = 2
private const val MAX_IME_WAIT_FRAME_COUNT = 60
