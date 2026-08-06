package com.soma369.laimory.core.domain.provider

/** 위치 좌표를 사용자에게 표시할 주소로 변환하는 플랫폼 포트. */
fun interface LocationAddressResolver {
    /** 주소를 찾으면 공백이 아닌 문자열을 반환하고, 찾을 수 없거나 일시적으로 실패하면 null을 반환한다. */
    suspend fun resolve(
        latitude: Double,
        longitude: Double,
    ): String?
}
