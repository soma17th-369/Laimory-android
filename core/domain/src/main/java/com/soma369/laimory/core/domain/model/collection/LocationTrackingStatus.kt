package com.soma369.laimory.core.domain.model.collection

/**
 * 위치 자동 수집 중의 실시간 상태(라이브 표시용).
 *
 * 저장되는 [SourceItem] 중 체류는 기준 시간 도달 시 열린 상태로 확정·갱신되고, 이동은 다음 체류가 확정되거나
 * 추적이 끝날 때 마감된다. 이 상태는 저장 여부와 별개로 진행 중인 세그먼트를 UI에 즉시 보여준다.
 */
sealed interface LocationTrackingStatus {
    /** 현재 한 장소에 머무는 중. 경과 시간은 [sinceMillis]~[nowMillis] 에서 파생한다. */
    data class Dwelling(
        val latitude: Double,
        val longitude: Double,
        val sinceMillis: Long,
        val nowMillis: Long,
    ) : LocationTrackingStatus

    /** 현재 이동 중(장소를 벗어나 안착 전). */
    data object Moving : LocationTrackingStatus
}
