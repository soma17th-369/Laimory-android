package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface DraftConsentUiIntent : UiIntent {
    /**
     * 진입·복귀(ON_RESUME) 시 저장된 위치정보 약관 동의를 다시 판정한다.
     *
     * 제출이 403 `-3001` 로 막혀 약관 화면에 다녀오는 경로가 **같은 스냅샷으로 돌아온다** —
     * 사진을 다시 고르지 않게 하려고 준비를 폐기하지 않기 때문이다. 새 생성 시도가 아니므로
     * 스냅샷 수집만으로는 재판정 계기가 없어, 화면이 복귀를 알려 준다.
     */
    data object Sync : DraftConsentUiIntent

    /** 유형 상세 화면으로 이동한다. 전송 0건 유형은 무시된다. */
    data class OpenTypeDetail(
        val group: DraftConsentTypeGroup,
    ) : DraftConsentUiIntent

    /**
     * 상세 항목의 전송 포함↔미포함을 전환한다.
     * 사진은 홈 선택이 정본이므로 무시되고, 제출 중에도 무시된다.
     */
    data class ToggleItemInclusion(
        val itemKey: String,
    ) : DraftConsentUiIntent

    /**
     * 위치정보 전송을 한 번에 켜고 끈다.
     *
     * 위치는 항목별 토글을 두지 않는다 — 지도와 목록 카드가 이미 상태를 보여주는데 카드마다
     * 토글을 또 얹으면 중첩된 semantics 로 포커스와 낭독이 두 번 생긴다. 제출 중에는 무시된다.
     */
    data object ToggleLocationInclusion : DraftConsentUiIntent

    /** 유형 상세 화면에서 동의 화면으로 복귀한다. 준비 상태는 유지된다. */
    data object CloseTypeDetail : DraftConsentUiIntent
}
