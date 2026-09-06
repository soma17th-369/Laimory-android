package com.soma369.laimory.feature.terms.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class TermsUiState(
    val gate: TermsGateState = TermsGateState.Unknown,
    /** 동의받아야 하는 이용약관. 판정이 서기 전이거나 통과 상태면 `null` 이다. */
    val termsOfService: TermDocument? = null,
    /** 동의 대상이 아니라 함께 안내하는 처리방침. 없으면 링크가 눌리지 않는다. */
    val privacyPolicy: TermDocument? = null,
    /**
     * 단계 동의 모드에서 아직 받아야 하는 문서.
     *
     * 비어 있고 [isStageMode] 가 참이면 받을 것이 없다는 뜻이라 화면이 곧바로 닫힌다 — 열어 두면
     * 아무 항목도 없는 동의 화면이 남는다.
     */
    val stageDocuments: List<TermDocument> = emptyList(),
    val checkedStageTerms: Set<TermType> = emptySet(),
    /** 초안 생성 단계 동의를 받으러 열렸는지. 로그인 단계 화면과 문구·행동이 다르다. */
    val isStageMode: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) : UiState {
    // 단계 모드는 로그인 게이트 판정을 기다리지 않는다 — 이 계정은 이미 이용약관을 지나 왔다.
    val isLoading: Boolean get() = !isStageMode && gate == TermsGateState.Unknown

    val hasFailed: Boolean get() = !isStageMode && gate == TermsGateState.Failed

    /**
     * 만 14세 이상 확인은 여기서 받지 않는다.
     *
     * 온보딩 마지막 장이 필수 확인 목록에서 함께 받고 **기록까지 남긴다** — 두 자리에서 물으면
     * 같은 질문을 두 번 받게 되고, 이 화면의 확인은 어디에도 남지 않아 근거가 되지도 않는다.
     */
    val canAgree: Boolean
        get() =
            when {
                isSubmitting -> false
                // 단계 모드는 목록의 모든 항목을 확인해야 넘어간다.
                isStageMode -> stageDocuments.isNotEmpty() && stageDocuments.all { it.termType in checkedStageTerms }
                else -> termsOfService != null
            }
}
