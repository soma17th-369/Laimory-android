package com.soma369.laimory.core.ui.terms

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.soma369.laimory.core.domain.model.terms.TermDocument

/**
 * 문장 안에 약관 원문 링크 한 조각을 넣는다.
 *
 * [document] 가 `null` 이면 **누를 수 없는 평범한 글자**로 그린다. catalog 가 아직 활성화되지
 * 않았거나 조회가 실패한 경우인데, 밑줄만 남기고 눌러도 아무 일이 없으면 고장난 링크가 된다.
 */
fun AnnotatedString.Builder.appendTermLink(
    label: String,
    document: TermDocument?,
    linkStyle: SpanStyle,
    onOpen: (TermDocument) -> Unit,
) {
    if (document == null) {
        append(label)
        return
    }
    val link =
        LinkAnnotation.Clickable(
            tag = document.termType.name,
            styles = TextLinkStyles(style = linkStyle.copy(textDecoration = TextDecoration.Underline)),
        ) { onOpen(document) }
    withLink(link) { append(label) }
}

/** 링크가 아닌 부분에 같은 스타일을 주고 싶을 때 쓴다. */
fun AnnotatedString.Builder.appendStyled(
    text: String,
    style: SpanStyle,
) {
    withStyle(style) { append(text) }
}
