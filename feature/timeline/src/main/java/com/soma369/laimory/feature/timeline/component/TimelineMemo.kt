package com.soma369.laimory.feature.timeline.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.soma369.laimory.core.domain.model.timeline.TimelineEventMemoPolicy
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.core.ui.theme.laimorySignature
import com.soma369.laimory.feature.timeline.model.MEMO_PROMPT
import com.soma369.laimory.feature.timeline.model.TimelineMemoDisplay
import com.soma369.laimory.feature.timeline.model.timelineMemoDisplay
import com.soma369.laimory.feature.timeline.model.timelineMemoQuestion
import com.soma369.laimory.feature.timeline.state.TimelineMemoEditorState
import com.soma369.laimory.core.ui.R as UiR

/**
 * 이벤트 메모.
 *
 * AI 질문은 **메모 위 말풍선**으로 따로 선다. 예전에는 메모가 비었을 때만 질문이 메모 자리에
 * 들어앉아, 답을 적기 시작하면 무엇을 물었는지 볼 수 없었다.
 *
 * 메모 자리의 왼쪽 표시는 하나만 강조색을 쓴다 — **지금 커서가 있는 자리**다.
 *
 * | 상태 | 표시 | 색 |
 * | --- | --- | --- |
 * | 아직 안 쓴 자리 | 밑줄 1 | `outline` |
 * | 쓰는 중 | 밑줄 2 | `primary` |
 * | 다 쓴 메모 | 인용 획 2 | `outline` |
 *
 * 다 쓴 메모의 인용 획은 읽기 모드와 같은 색이다. 모드를 오가도 이미 쓴 글의 겉모습은 흔들리지
 * 않는다.
 */
@Composable
internal fun TimelineMemo(
    memo: String?,
    question: String?,
    editor: TimelineMemoEditorState?,
    isEditable: Boolean,
    onClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
) {
    val bubbleQuestion = timelineMemoQuestion(question = question, isEditable = isEditable)
    val display = timelineMemoDisplay(memo = memo, isEditable = isEditable)
    if (bubbleQuestion == null && display == null && editor == null) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(QUESTION_GAP),
    ) {
        bubbleQuestion?.let { MemoQuestionBubble(question = it) }
        when {
            editor != null ->
                MemoInputLine(
                    editor = editor,
                    placeholder = MEMO_PROMPT,
                    onValueChange = onValueChange,
                    onCommit = onCommit,
                )

            display is TimelineMemoDisplay.Memo ->
                MemoQuote(
                    text = display.text,
                    onClick = onClick.takeIf { isEditable },
                    topPadding = if (isEditable) MEMO_LINE_TOP_PADDING else QUOTE_TOP_PADDING,
                )

            display is TimelineMemoDisplay.Prompt ->
                MemoPromptLine(placeholder = display.text, onClick = onClick)

            else -> Unit
        }
    }
}

/**
 * AI 질문 말풍선.
 *
 * 좌상 모서리만 4 인 것은 꼬리 자리다. 아이콘은 말풍선 안 첫 줄에 물리며 본문에 들여쓰기를
 * 만들지 않는다 — 질문이 여러 줄로 접혀도 둘째 줄부터는 왼쪽 선에 맞는다.
 *
 * **아이콘과 글자는 baseline 으로 맞춘다.** 글자를 담는 상자(`lineHeight`)는 글리프보다 크고,
 * 남는 여백이 위아래로 똑같이 붙지 않는다 — 한글은 디센더를 거의 쓰지 않아 아래가 더 남는다.
 * 그래서 상자끼리 가운데를 맞추면(`Alignment.CenterVertically`) 아이콘이 2dp 가량 처져 보인다.
 * 눈은 상자가 아니라 글자를 본다.
 *
 * [RowScope.alignByBaseline] 은 **첫 줄** baseline 을 쓰므로, 질문이 여러 줄로 접혀도 아이콘은
 * 첫 줄에 물린 채 남는다. 가운데 정렬이었다면 줄이 늘어날수록 아래로 흘러내린다.
 *
 * TalkBack 은 말풍선을 한 덩어리로 읽는다. 아이콘은 뜻을 더하지 않는 장식이라 이름이 없고,
 * 대신 이것이 AI 가 던진 질문이라는 사실을 문장 앞에 붙인다.
 */
@Composable
private fun MemoQuestionBubble(question: String) {
    val questionStyle = MaterialTheme.typography.bodyMedium
    Row(
        modifier =
            Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = BUBBLE_TAIL_CORNER,
                        topEnd = BUBBLE_CORNER,
                        bottomEnd = BUBBLE_CORNER,
                        bottomStart = BUBBLE_CORNER,
                    ),
                ).background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = BUBBLE_PADDING_HORIZONTAL, vertical = BUBBLE_PADDING_VERTICAL)
                .semantics(mergeDescendants = true) { contentDescription = "AI 질문, $question" },
        horizontalArrangement = Arrangement.spacedBy(BUBBLE_ICON_GAP),
    ) {
        Icon(
            painter = painterResource(UiR.drawable.ico_default_sparkle),
            contentDescription = null,
            // baseline 에 얹을 지점을 아이콘 위에서 3/4 되는 곳으로 잡는다. 그러면 아이콘 가운데가
            // baseline 보다 아이콘 높이의 1/4 만큼 위에 서는데, 그 자리가 대문자·한글이 차지하는
            // 띠의 한가운데다. 글꼴을 키우면 아이콘과 글자가 함께 커지므로 비율이 그대로 산다.
            modifier = Modifier.size(BUBBLE_ICON_SIZE).alignBy { it.measuredHeight * 3 / 4 },
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = question,
            modifier = Modifier.alignByBaseline(),
            style = questionStyle,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = QUESTION_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 다 쓴 메모. 편집 모드에서만 눌린다 — 읽기 모드의 메모는 본문의 한 문단이라 누를 곳이 없다.
 *
 * 위 여백은 모드마다 시안이 다르다. 편집 모드는 입력 줄과 같은 8(MemoAnswer Filled)이라 쓰기 전과 뒤의
 * 자리가 같고, 읽기 모드는 본문 문단에 붙는 2(MemoQuote)다.
 */
@Composable
private fun MemoQuote(
    text: String,
    onClick: (() -> Unit)?,
    topPadding: Dp,
) {
    Text(
        text = text,
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClickLabel = "메모 편집", onClick = onClick) else Modifier)
                .memoQuote(MaterialTheme.colorScheme.outline, topPadding),
        style = memoTextStyle(),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = MEMO_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 아직 비어 있는 메모 자리. 인용이 아니라 입력칸이라 밑줄을 깐다.
 *
 * 안내와 밑줄을 통째로 옅게 둔다(시안 70%) — 이미 쓴 글과 한눈에 갈린다. 위 여백까지 누를 수 있게
 * `clickable` 을 여백보다 앞에 둔다.
 */
@Composable
private fun MemoPromptLine(
    placeholder: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "메모 작성", onClick = onClick)
                .padding(top = MEMO_LINE_TOP_PADDING)
                .alpha(PROMPT_ALPHA),
        verticalArrangement = Arrangement.spacedBy(INPUT_LINE_GAP),
    ) {
        MemoPlaceholder(text = placeholder, modifier = Modifier.fillMaxWidth())
        MemoUnderline(color = MaterialTheme.colorScheme.outline, thickness = UNDERLINE_IDLE)
    }
}

/**
 * 빈 메모 자리의 안내 — 연필 아이콘이 **글자처럼** 문장 앞에 붙는다.
 *
 * 아이콘을 옆 칸이 아니라 글자 자리([InlineTextContent])에 둔다. 문장과 한 몸이라 글꼴 배율을
 * 키우면 함께 커지고, 좁은 폭에서 접혀도 둘째 줄이 아이콘 밑이 아니라 왼쪽 선에서 시작한다.
 *
 * 누르기 전 자리([MemoPromptLine])와 누른 뒤 빈 입력칸([MemoInputLine])이 이것 하나를 쓴다. 따로
 * 그리면 누르는 순간 아이콘이 사라지거나 글자가 옆으로 튄다. 투명도(시안 70%)는 부르는 쪽이 준다 —
 * 누르기 전 자리는 밑줄까지 함께 옅어지고, 입력 중에는 밑줄이 강조색이라 안내만 옅어진다.
 *
 * 좌우 여백(시안 4dp)은 입력칸 커서의 자리이기도 하다. 빈 입력칸의 커서는 맨 왼쪽에 서므로 여백이
 * 없으면 아이콘 위에서 깜빡인다.
 *
 * TalkBack 은 아이콘을 읽지 않는다 — 글자 자리의 대체 문자를 공백으로 둔다. 기본값(`�`)을 두면
 * 알 수 없는 문자로 읽힌다.
 */
@Composable
private fun MemoPlaceholder(
    text: String,
    modifier: Modifier = Modifier,
) {
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text =
            buildAnnotatedString {
                appendInlineContent(PLACEHOLDER_ICON_ID, alternateText = " ")
                append(text)
            },
        modifier = modifier.padding(horizontal = Spacing.extraSmall),
        style = memoTextStyle(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        inlineContent =
            mapOf(
                PLACEHOLDER_ICON_ID to
                    InlineTextContent(
                        Placeholder(
                            width = (PLACEHOLDER_ICON_EM + PLACEHOLDER_ICON_GAP_EM).em,
                            height = PLACEHOLDER_ICON_EM.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                    ) {
                        // 글자 자리의 앞쪽에 정사각형으로 앉힌다. 남는 뒤쪽이 글자와의 간격이 된다.
                        Icon(
                            painter = painterResource(UiR.drawable.ico_default_pen),
                            contentDescription = null,
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                            tint = iconTint,
                        )
                    },
            ),
    )
}

/**
 * 입력 중인 메모.
 *
 * **포커스가 빠지면 저장한다.** 확인 버튼이 따로 없으므로 이 콜백이 유일한 저장 신호다. 처음
 * 포커스를 잡기 전에도 `onFocusChanged` 가 한 번 도는데, 그때는 커밋하지 않는다 — 편집기를 열자마자
 * 스스로 닫힌다.
 */
@Composable
private fun MemoInputLine(
    editor: TimelineMemoEditorState,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
) {
    val focusRequester = remember(editor.timelineEventId) { FocusRequester() }
    val bringIntoViewRequester = remember(editor.timelineEventId) { BringIntoViewRequester() }
    var hasFocused by remember(editor.timelineEventId) { mutableStateOf(false) }
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
    LaunchedEffect(editor.timelineEventId) {
        focusRequester.requestFocus()
        imeInsets.awaitSettled(density)
        withFrameNanos { }
        bringIntoViewRequester.bringIntoView()
    }
    // 키보드를 내리는 것도 이 자리에서 손을 떼는 동작이다. `BasicTextField` 는 키보드가 내려가도
    // 입력 포커스를 쥐고 있어서 `onFocusChanged` 만으로는 이 경로가 잡히지 않는다 — 키보드의
    // 숨기기 버튼으로 내리면 편집기도 쓰던 글도 그대로 남는다. 뒤로 키도 마찬가지다: 키보드가
    // 올라와 있으면 시스템이 먼저 먹고 화면의 `BackHandler` 까지 오지 않는다.
    //
    // **한 번 올라온 적 있는 키보드가 내려가는 전환만** 센다. 편집기를 여는 순간에는 아직 inset
    // 이 0 이라 그대로 두면 열자마자 스스로 닫히고, 메모 A→B 로 옮길 때는 키보드가 계속 올라와
    // 있어 애초에 전환이 없다.
    LaunchedEffect(editor.timelineEventId) {
        var wasImeVisible = false
        snapshotFlow { imeInsets.getBottom(density) > 0 }
            .collect { isImeVisible ->
                if (isImeVisible) {
                    wasImeVisible = true
                } else if (wasImeVisible) {
                    onCommit()
                }
            }
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
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoViewRequester).padding(top = MEMO_LINE_TOP_PADDING),
        verticalArrangement = Arrangement.spacedBy(INPUT_LINE_GAP),
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { value ->
                val limited = value.limitedToMemoLength()
                textFieldValue = limited
                onValueChange(limited.text)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = EDITOR_MIN_HEIGHT, max = EDITOR_MAX_HEIGHT)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            hasFocused = true
                        } else if (hasFocused) {
                            onCommit()
                        }
                    },
            textStyle = memoTextStyle().copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            maxLines = EDITOR_MAX_LINES,
            decorationBox = { innerTextField ->
                Box {
                    if (textFieldValue.text.isEmpty()) {
                        MemoPlaceholder(text = placeholder, modifier = Modifier.alpha(PROMPT_ALPHA))
                    }
                    innerTextField()
                }
            },
        )
        MemoUnderline(color = MaterialTheme.colorScheme.primary, thickness = UNDERLINE_ACTIVE)
    }
}

@Composable
private fun MemoUnderline(
    color: Color,
    thickness: Dp,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(thickness)
                .background(color),
    )
}

/**
 * 상한을 넘긴 입력을 잘라 낸다.
 *
 * 글자수 표시가 없으므로 넘긴 사실을 나중에 알릴 방법이 없다. 붙여넣기를 통째로 거절하는 대신
 * 상한까지만 받는다 — 거절하면 무엇이 왜 안 들어갔는지 알 길이 없다.
 */
private fun TextFieldValue.limitedToMemoLength(): TextFieldValue {
    if (text.length <= TimelineEventMemoPolicy.MAX_LENGTH) return this
    val limited = text.take(TimelineEventMemoPolicy.MAX_LENGTH)
    return copy(
        text = limited,
        selection = TextRange(selection.start.coerceAtMost(limited.length), selection.end.coerceAtMost(limited.length)),
    )
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

/**
 * 인용 표시 — 왼쪽 세로 획과 그만큼의 안쪽 여백.
 *
 * 획을 자식으로 두고 높이를 맞추려면 `IntrinsicSize.Min` 이 필요한데, 그리는 대상이 여러 줄로
 * 접히는 본문이라 미리 재기 어렵다. 그래서 배경으로 직접 그린다 — 어떤 내용이 오든 그려진
 * 높이가 곧 내용의 높이다.
 */
private fun Modifier.memoQuote(
    color: Color,
    topPadding: Dp,
): Modifier =
    padding(top = topPadding)
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
private fun memoTextStyle() =
    MaterialTheme.laimorySignature.note.copy(
        fontSize = MEMO_FONT_SIZE,
        lineHeight = MEMO_LINE_HEIGHT,
    )

private const val MEMO_MAX_LINES = 3

/** question 은 서버 기준 255자까지 온다. 다 펼치면 말풍선이 카드를 덮는다. */
private const val QUESTION_MAX_LINES = 5

private val MEMO_FONT_SIZE = 20.sp
private val MEMO_LINE_HEIGHT = 22.sp

/** 말풍선과 메모 사이. */
private val QUESTION_GAP = 12.dp
private val BUBBLE_CORNER = 12.dp
private val BUBBLE_TAIL_CORNER = 4.dp
private val BUBBLE_PADDING_HORIZONTAL = 12.dp
private val BUBBLE_PADDING_VERTICAL = 8.dp
private val BUBBLE_ICON_GAP = 8.dp
private val BUBBLE_ICON_SIZE = 16.dp

/** 입력 줄 — 본문과 밑줄 사이 간격, 그리고 두 굵기. */
private val INPUT_LINE_GAP = 8.dp
private val UNDERLINE_IDLE = 1.dp
private val UNDERLINE_ACTIVE = 2.dp

/** 인용 획. 시안 폭 2, 모서리 1, 본문과의 간격 10, 위 여백 2(읽기 모드 — 편집 모드는 [MEMO_LINE_TOP_PADDING]). */
private val QUOTE_RULE_WIDTH = 2.dp
private val QUOTE_RULE_RADIUS = 1.dp
private val QUOTE_RULE_GAP = 10.dp
private val QUOTE_TOP_PADDING = 2.dp

/** 편집 모드 메모 줄의 위 여백. 시안 MemoAnswer 의 input-line·memo-quote `pt spacing/8`. */
private val MEMO_LINE_TOP_PADDING = Spacing.small

/** 한 줄 높이. 빈 입력칸이 접히지 않게 잡아 둔다. */
private val EDITOR_MIN_HEIGHT = 22.dp
private val EDITOR_MAX_HEIGHT = 160.dp
private const val EDITOR_MAX_LINES = 8

private const val STABLE_IME_FRAME_COUNT = 2
private const val MAX_IME_WAIT_FRAME_COUNT = 60

/**
 * 안내 문구 앞 아이콘 자리. 크기를 메모 글꼴에 대한 배수(`em`)로 잡아 글꼴 배율을 따라간다.
 *
 * 메모 글꼴 20sp 에서 아이콘 1em = 20(시안), 뒤쪽 간격 0.2em = 4(시안).
 */
private const val PLACEHOLDER_ICON_ID = "memo-placeholder-icon"
private const val PLACEHOLDER_ICON_EM = 1f
private const val PLACEHOLDER_ICON_GAP_EM = 0.2f

/** 빈 자리 안내의 투명도. 시안 opacity 70. */
private const val PROMPT_ALPHA = 0.7f
