package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface DraftConsentUiIntent : UiIntent {
    data class ToggleTerm(
        val term: DraftConsentTerm,
    ) : DraftConsentUiIntent

    data class OpenTypeDetail(
        val group: DraftConsentTypeGroup,
    ) : DraftConsentUiIntent

    data object CloseTypeDetail : DraftConsentUiIntent

    data class OpenTermsDetail(
        val term: DraftConsentTerm,
    ) : DraftConsentUiIntent

    data object CloseTermsDetail : DraftConsentUiIntent

    /** 동의 완료 후 생성 CTA. 스냅샷 그대로 사진 업로드·초안 생성을 시작한다. */
    data object Submit : DraftConsentUiIntent

    /** 뒤로가기. 준비 상태를 폐기하고 홈으로 복귀한다. 제출 중에는 무시된다. */
    data object NavigateBack : DraftConsentUiIntent
}
