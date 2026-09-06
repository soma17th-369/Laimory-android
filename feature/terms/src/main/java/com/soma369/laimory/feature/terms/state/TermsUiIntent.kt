package com.soma369.laimory.feature.terms.state

import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.base.UiIntent

sealed interface TermsUiIntent : UiIntent {
    /**
     * 단계 동의 모드로 연다. 로그인 단계 화면은 이 의도를 보내지 않는다.
     *
     * 어느 단계가 비었는지는 서버 오류가 알려 주지 않으므로 **후보를 모두** 받아 여기서 다시
     * 조회하고, 실제로 남은 문서만 화면에 올린다.
     */
    data class InitializeStages(val stages: List<TermStage>) : TermsUiIntent

    /** 단계 동의 목록의 항목 하나를 켜고 끈다. */
    data class StageTermToggled(val termType: TermType) : TermsUiIntent

    data object AgreeClicked : TermsUiIntent

    /** 판정 조회가 실패했을 때 다시 묻는다. */
    data object RetryClicked : TermsUiIntent

    /** 다른 계정으로 들어갈 수 있게 남겨 두는 길. 약관 화면에 갇히지 않게 한다. */
    data object LogoutClicked : TermsUiIntent
}
