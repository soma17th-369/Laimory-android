package com.soma369.laimory.core.domain.usecase

/**
 * MOVEMENT 한 건의 출발·도착 주소.
 *
 * 한 `rawId`에 점이 둘이라 하나의 값으로는 어느 쪽인지 말할 수 없다. 해석하지 못한 쪽은 null 이고,
 * 둘 다 null 일 수 있다.
 */
data class ResolvedMovementAddresses(
    val start: String?,
    val end: String?,
)
