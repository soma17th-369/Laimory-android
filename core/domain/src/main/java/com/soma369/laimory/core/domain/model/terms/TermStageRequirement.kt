package com.soma369.laimory.core.domain.model.terms

/**
 * 한 단계의 동의 요구 상태.
 *
 * [items] 에는 **조회된 문서만** 들어간다. 서버 catalog 가 아직 활성화되지 않은 종류는 아예
 * 빠지므로, 비어 있으면 [isSatisfied] 가 참이 되어 단계가 열린다 — 서버의 fail-open 과 같은 판정이다.
 */
data class TermStageRequirement(
    val stage: TermStage,
    val items: List<TermRequirement>,
) {
    val isSatisfied: Boolean get() = items.all { it.isAgreed }

    /** 아직 동의가 없거나 개정으로 버전이 어긋난 문서. 화면이 기본 해제로 다시 받아야 하는 것들이다. */
    val pending: List<TermDocument> get() = items.filterNot { it.isAgreed }.map { it.document }
}
