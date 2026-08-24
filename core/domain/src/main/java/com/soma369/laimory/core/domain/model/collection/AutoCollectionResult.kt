package com.soma369.laimory.core.domain.model.collection

/**
 * 자동 수집 한 번의 결과.
 *
 * 빈 [outcomes] 만으로는 "다시 볼 유형이 없어 곧장 통과" 와 "기다리다 상한을 넘김" 을 구분할 수
 * 없다. 앞은 정상 경로라 조용해야 하고 뒤는 사용자에게 알려야 해서 [timedOut] 을 따로 둔다.
 */
data class AutoCollectionResult(
    /** 이번에 실제로 수집을 시도한 유형의 결과. 모두 최근이면 비어 있다. */
    val outcomes: Map<ItemType, AutoCollectionOutcome> = emptyMap(),
    /** 기다림이 상한을 넘겨 결과를 못 받았는지. 수집 자체는 계속 돌고 있다. */
    val timedOut: Boolean = false,
) {
    /**
     * 최신 상태를 확보하지 못했는지. 사용자에게 알릴지 판단하는 값이다.
     *
     * 권한 없음·미지원은 포함하지 않는다 — 사용자가 그렇게 설정한 정상 상태라, 생성할 때마다
     * 알리면 소음이 된다.
     */
    val isIncomplete: Boolean
        get() = timedOut || outcomes.values.any { it is AutoCollectionOutcome.Failed }
}
