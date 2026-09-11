package com.soma369.laimory.core.domain.provider

import com.soma369.laimory.core.domain.model.collection.ResolvedAddress

/** 위치 좌표를 사용자에게 표시할 주소로 변환하는 플랫폼 포트. */
fun interface LocationAddressResolver {
    /** 주소를 찾으면 [ResolvedAddress] 를, 찾을 수 없거나 일시적으로 실패하면 null 을 반환한다. */
    suspend fun resolve(
        latitude: Double,
        longitude: Double,
    ): ResolvedAddress?
}
