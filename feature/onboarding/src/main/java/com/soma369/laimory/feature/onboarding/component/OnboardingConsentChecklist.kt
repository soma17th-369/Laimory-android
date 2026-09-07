package com.soma369.laimory.feature.onboarding.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ButtonDefaults
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
 * 약관 항목의 이름은 서버가 준 제목을 그대로 쓰고, 원문은 게시된 주소를 연다. 앱이 문구를 따로
 * 들고 있으면 실제 동의한 내용과 화면이 갈린다.
 *
 * **연령 확인만 예외로 앱이 문구를 갖는다.** 서버 catalog 의 항목이 아니라 열람할 원문이 없는
 * 단순 확인이라 `보기` 가 붙지 않고, 목록이 비어 있어도(이미 다 동의했거나 catalog 가 아직
 * 없어도) 이 줄은 남는다.
 */
@Composable
internal fun OnboardingConsentChecklist(
    documents: List<TermDocument>,
    checked: Set<TermType>,
    /** 이미 동의해 되돌릴 수 없는 항목. 체크된 채로 두되 끄지는 못한다. */
    locked: Set<TermType>,
    isAgeConfirmed: Boolean,
    isEnabled: Boolean,
    errorMessage: String?,
    onToggle: (TermType) -> Unit,
    onToggleAge: () -> Unit,
    onOpenTerm: (TermDocument) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        // 맨 위에 둔다. 나이는 약관을 읽기 전에 정해져 있는 사실이라, 읽고 판단하는 항목들보다
        // 앞에 오는 편이 순서가 자연스럽다.
        ChecklistRow(
            label = AGE_CONFIRMATION_LABEL,
            isChecked = isAgeConfirmed,
            isEnabled = isEnabled,
            isToggleable = true,
            onToggle = onToggleAge,
        )
        documents.forEach { document ->
            ChecklistRow(
                label = "[필수] ${document.title}",
                isChecked = document.termType in checked,
                isEnabled = isEnabled,
                // 잠긴 항목은 체크만 막는다. 원문 보기까지 함께 끄면 무엇에 동의했는지
                // 확인할 길이 사라진다 — 되돌릴 수 없는 동의일수록 읽을 수 있어야 한다.
                isToggleable = document.termType !in locked,
                onToggle = { onToggle(document.termType) },
                trailing = {
                    // 원문 열기는 별도 버튼이다. 행 전체가 토글이라 여기서 클릭을 잡아 주지 않으면
                    // 읽으려다 동의가 켜진다.
                    TextButton(
                        onClick = { onOpenTerm(document) },
                        // 행이 높이를 정하므로 버튼은 그 안에서 가운데 정렬만 하면 된다.
                        modifier = Modifier.height(CHECKLIST_ROW_HEIGHT),
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
                },
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
private fun ChecklistRow(
    label: String,
    isChecked: Boolean,
    isEnabled: Boolean,
    isToggleable: Boolean,
    onToggle: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                // 모든 줄이 **같은 높이로 고정**된다. 최소 높이로 두면 `보기` 버튼이 있는 줄만
                // 그 크기에 끌려 올라가 한 목록 안에서 줄 간격이 들쭉날쭉해진다.
                .height(CHECKLIST_ROW_HEIGHT)
                // 글자까지 터치 영역에 넣는다. 체크박스만 누르게 하면 눌러야 할 곳이 너무 작다.
                .toggleable(
                    value = isChecked,
                    enabled = isEnabled && isToggleable,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ),
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
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // `보기` 가 없는 줄도 그 자리를 폭만큼 비워 둔다. 안 그러면 그 줄만 글자가 오른쪽 끝까지
        // 늘어나 다른 줄과 끝선이 어긋난다. **높이는 여기서 맞추지 않는다** — 행이 스스로
        // 높이를 갖는다.
        trailing?.invoke() ?: Spacer(modifier = Modifier.width(ButtonDefaults.MinWidth))
    }
}

/** 서버 문서가 아니라 앱이 받는 확인이라 문구를 여기서 갖는다. */
private const val AGE_CONFIRMATION_LABEL = "[필수] 만 14세 이상입니다"

/**
 * 목록 한 줄의 높이.
 *
 * **최소가 아니라 고정이다.** `보기` 버튼이 있는 줄과 없는 줄이 같은 높이여야 하는데, 최소 높이로
 * 두면 버튼이 있는 줄만 버튼 크기에 끌려 올라가 간격이 어긋난다. 터치 영역으로도 넉넉한 값이다.
 */
private val CHECKLIST_ROW_HEIGHT = 48.dp

/** 기본 48dp 터치 영역을 그대로 두면 몇 줄만으로 화면을 넘긴다. 그림만 줄이고 터치는 행이 받는다. */
private val CONSENT_CHECKBOX_SIZE = 18.dp
