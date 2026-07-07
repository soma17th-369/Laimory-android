package com.soma369.laimory.core.domain.model.collection

/**
 * 위치 자동 수집 중의 실시간 상태(라이브 표시용).
 *
 * 저장되는 [SourceItem](체류/이동)은 세그먼트가 닫힐 때 확정되지만, 이 상태는 진행 중인 세그먼트를 UI 에
 * 즉시 보여주기 위한 것이다.
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
