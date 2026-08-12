package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState

/**
 * 데이터 전송 확인·동의 화면의 UI 상태.
 *
 * [content]가 null 이면 현재 생성 시도의 준비물이 없는 상태다(프로세스 재생성 등) —
 * 스냅샷을 복원하지 않고 홈에서 다시 준비하도록 안내한다.
 * 체크 상태는 현재 생성 시도에만 유효하며 새 스냅샷이 들어오면 초기화된다.
 */
@Immutable
data class DraftConsentUiState(
    val content: DraftConsentUiContent? = null,
    val checkedTerms: Set<DraftConsentTerm> = emptySet(),
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val openTermsDetail: DraftConsentTerm? = null,
    /** 동의 문구 법무 확정 전 배포 가드 — false 면 모든 동의를 완료해도 제출할 수 없다. */
    val isSubmissionAllowed: Boolean = true,
) : UiState {
    val isAllTermsChecked: Boolean
        get() = checkedTerms.size == DraftConsentTerm.entries.size

    /** 필수 동의를 모두 완료하기 전에는 생성 CTA 를 비활성화한다. */
    val canSubmit: Boolean
        get() = content != null && isAllTermsChecked && !isSubmitting && isSubmissionAllowed
}
