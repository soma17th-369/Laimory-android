package com.soma369.laimory.feature.terms.state

import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.ui.base.UiIntent

sealed interface TermsUiIntent : UiIntent {
    /**
     * 로그인 단계 화면으로 연다. 단계 모드에서 남은 상태를 비운다.
     *
     * ViewModel 이 Activity 범위라 단계 화면과 **같은 인스턴스를 공유한다.** 이 의도가 없으면
     * 단계 동의를 한 번 연 뒤로는 로그아웃·계정 전환으로 이용약관이 필요해져도 초안 동의 화면이
     * 그대로 남는다.
     */
    data object InitializeLogin : TermsUiIntent

    /**
     * 단계 동의 모드로 연다.
     *
     * 어느 단계가 비었는지는 서버 오류가 알려 주지 않으므로 **후보를 모두** 받아 여기서 다시
     * 조회하고, 실제로 남은 문서만 화면에 올린다. **열 때마다 다시 조회한다** — 닫았다 다시 연
     * 사이에 동의가 기록됐을 수 있다.
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
