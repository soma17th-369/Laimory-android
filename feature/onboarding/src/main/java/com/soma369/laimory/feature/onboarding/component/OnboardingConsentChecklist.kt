package com.soma369.laimory.feature.onboarding.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * 동의 장의 필수 항목 목록.
 *
 * 항목마다 체크를 받지 않는다. 무엇에 동의하는지 보여 주는 자리이고, 동의 행위는
 * `모두 동의하고 시작하기` 버튼이 맡는다 — 결과가 분명한 버튼 쪽이 의사가 또렷하다.
 * **기본값은 해제이고**, 체크는 그 버튼을 누른 결과로만 차오른다.
 *
 * 항목 이름은 서버가 준 제목을 그대로 쓰고, 원문은 게시된 주소를 연다. 앱이 문구를 따로 들고
 * 있으면 실제 동의한 내용과 화면이 갈린다.
 */
@Composable
internal fun OnboardingConsentChecklist(
    documents: List<TermDocument>,
    checked: Set<TermType>,
    isEnabled: Boolean,
    errorMessage: String?,
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
    onOpenTerm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        // 표시 전용이다. 누를 수 있게 두면 버튼이 다시 채우는 값을 사용자가 되돌리는 꼴이 된다.
        Checkbox(checked = isChecked, onCheckedChange = null, enabled = isEnabled)
        Text(
            modifier = Modifier.weight(1f),
            text = "[필수] ${document.title}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // 원문 열기만 누를 수 있다.
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
