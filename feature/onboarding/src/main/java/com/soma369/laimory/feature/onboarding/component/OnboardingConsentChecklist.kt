package com.soma369.laimory.feature.onboarding.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * 동의 장의 필수 항목 목록.
 *
 * 항목을 하나씩 켜고 끌 수 있고, `모두 동의하고 시작하기` 는 남은 것을 한꺼번에 채운다 —
 * 어느 쪽이든 결과가 분명하다. **기본값은 해제다.**
 *
 * 항목 이름은 서버가 준 제목을 그대로 쓰고, 원문은 게시된 주소를 연다. 앱이 문구를 따로 들고
 * 있으면 실제 동의한 내용과 화면이 갈린다.
 */
@Composable
internal fun OnboardingConsentChecklist(
    documents: List<TermDocument>,
    checked: Set<TermType>,
    /** 이미 동의해 되돌릴 수 없는 항목. 체크된 채로 두되 끄지는 못한다. */
    locked: Set<TermType>,
    isEnabled: Boolean,
    errorMessage: String?,
    onToggle: (TermType) -> Unit,
    onOpenTerm: (TermDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        documents.forEach { document ->
            ConsentRow(
                document = document,
                isChecked = document.termType in checked,
                isEnabled = isEnabled,
                // 잠긴 항목은 체크만 막는다. 원문 보기까지 함께 끄면 무엇에 동의했는지
                // 확인할 길이 사라진다 — 되돌릴 수 없는 동의일수록 읽을 수 있어야 한다.
                isToggleable = document.termType !in locked,
                onToggle = { onToggle(document.termType) },
                onOpenTerm = { onOpenTerm(document) },
            )
        }
        errorMessage?.let { message ->
            Text(
                modifier = Modifier.padding(top = Spacing.small),
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ConsentRow(
    document: TermDocument,
    isChecked: Boolean,
    isEnabled: Boolean,
    isToggleable: Boolean,
    onToggle: () -> Unit,
    onOpenTerm: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                // 글자까지 터치 영역에 넣는다. 체크박스만 누르게 하면 눌러야 할 곳이 너무 작다.
                .toggleable(
                    value = isChecked,
                    enabled = isEnabled && isToggleable,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ).padding(vertical = CONSENT_ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        // 행 전체가 토글이라 체크박스는 그림만 맡는다. 둘 다 누르면 두 번 뒤집힌다.
        Checkbox(
            modifier = Modifier.size(CONSENT_CHECKBOX_SIZE),
            checked = isChecked,
            onCheckedChange = null,
            enabled = isEnabled && isToggleable,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = "[필수] ${document.title}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // 원문 열기는 별도 버튼이다. 행 전체가 토글이라 여기서 클릭을 잡아 주지 않으면
        // 읽으려다 동의가 켜진다.
        TextButton(
            onClick = onOpenTerm,
            enabled = isEnabled,
            contentPadding = PaddingValues(horizontal = Spacing.small),
        ) {
            Text(
                modifier = Modifier.semantics { contentDescription = "${document.title} 전문 보기" },
                text = "보기",
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

/**
 * 동의 한 줄의 상하 여백.
 *
 * 마지막 장은 이미지·설명·동의 네 줄이 한 화면에 들어가야 한다. 이 목록은 읽을거리가 아니라
 * 확인하고 넘기는 줄이라 본문만큼 클 이유가 없다. 다만 터치 영역은 행 전체가 토글이라
 * 여백을 줄여도 누를 곳은 넉넉하다.
 */
private val CONSENT_ROW_VERTICAL_PADDING = 5.dp

/** 기본 48dp 터치 영역을 그대로 두면 네 줄만으로 화면을 넘긴다. 그림만 줄이고 터치는 행이 받는다. */
private val CONSENT_CHECKBOX_SIZE = 18.dp
