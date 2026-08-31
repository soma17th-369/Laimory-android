package com.soma369.laimory.core.domain.model.terms

/**
 * 이용약관 단계의 판정 결과. 앱 루트가 이 값으로 갈린다.
 *
 * [Unknown] 과 [Failed] 를 나눈다 — 아직 모르는 것과 물어봤는데 실패한 것은 사용자에게 보여 줄
 * 것이 다르다. 모르는 동안에는 아무 루트도 열지 않고, 실패했으면 다시 시도할 자리를 준다.
 */
sealed interface TermsGateState {
    /** 아직 판정 전. 루트를 고르지 않는다. */
    data object Unknown : TermsGateState

    /**
     * 동의가 끝났거나 요구할 문서가 없다.
     *
     * 서버 catalog 가 활성화되기 전이면 요구가 비어 여기로 온다 — 서버도 같은 fail-open 이라
     * 판정이 어긋나지 않는다.
     */
    data object Satisfied : TermsGateState

    /** 동의가 필요하다. [documents] 는 기본 해제 상태로 다시 받아야 하는 문서다. */
    data class Required(val documents: List<TermDocument>) : TermsGateState

    /**
     * 조회 자체가 실패했다.
     *
     * 빈 catalog 로 치환하지 않는다. 열어 주면 운영 서버에서 이후 API 가 계속 거절당하고,
     * 계속 [Unknown] 으로 두면 앱이 로딩에서 나오지 못한다.
     */
    data object Failed : TermsGateState
}
