package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.collection.ResolvedAddress

/** 로컬 STAY payload의 주소만 원자적으로 갱신하는 저장 계약. */
interface StayAddressRepository {
    /**
     * [rawId]에 해당하는 STAY의 기존 수집 필드를 유지하면서 [address]를 저장한다.
     *
     * 한 줄 주소와 함께 시·동 층위도 저장한다 — 층위가 없으면 홈 위치 카드가 들어올 때마다 좌표를
     * 다시 해석하게 된다.
     *
     * @return 대상 STAY가 존재해 주소를 저장했으면 true
     */
    suspend fun updateAddress(
        rawId: String,
        address: ResolvedAddress,
    ): Boolean
}
