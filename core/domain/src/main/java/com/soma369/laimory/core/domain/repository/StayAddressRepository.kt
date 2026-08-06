package com.soma369.laimory.core.domain.repository

/** 로컬 STAY payload의 주소만 원자적으로 갱신하는 저장 계약. */
interface StayAddressRepository {
    /**
     * [rawId]에 해당하는 STAY의 기존 수집 필드를 유지하면서 [address]를 저장한다.
     *
     * @return 대상 STAY가 존재해 주소를 저장했으면 true
     */
    suspend fun updateAddress(
        rawId: String,
        address: String,
    ): Boolean
}
