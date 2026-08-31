package com.soma369.laimory.feature.onboarding.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * 동의 장의 필수 항목 목록.
 *
 * **기본값 체크는 두지 않는다** — 미리 체크된 동의는 능동적 의사 확인이 아니다.
 * 항목 이름은 서버가 준 제목을 그대로 쓰고, 원문은 게시된 주소를 연다. 앱이 문구를 따로 들고
 * 있으면 실제 동의한 내용과 화면이 갈린다.
 */
@Composable
internal fun OnboardingConsentChecklist(
    documents: List<TermDocument>,
    checked: Set<TermType>,
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
    onToggle: () -> Unit,
    onOpenTerm: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isChecked,
                    enabled = isEnabled,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ).padding(vertical = Spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        // 행 전체가 토글이라 체크박스는 그림만 맡는다. 둘 다 누르면 두 번 뒤집힌다.
        Checkbox(checked = isChecked, onCheckedChange = null, enabled = isEnabled)
        Text(
            modifier = Modifier.weight(1f),
            text = "[필수] ${document.title}",
            style = MaterialTheme.typography.bodyMedium,
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
