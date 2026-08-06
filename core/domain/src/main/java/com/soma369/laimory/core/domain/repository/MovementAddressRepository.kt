package com.soma369.laimory.core.domain.repository

/** 로컬 MOVEMENT payload의 출발·도착 주소를 원자적으로 갱신하는 저장 계약. */
interface MovementAddressRepository {
    /**
     * [rawId]에 해당하는 MOVEMENT의 기존 수집 필드를 유지하면서 해석에 성공한 주소를 저장한다.
     *
     * @return 대상 MOVEMENT가 존재하고 하나 이상의 주소를 저장했으면 true
     */
    suspend fun updateAddresses(
        rawId: String,
        startAddress: String?,
        endAddress: String?,
    ): Boolean
}
