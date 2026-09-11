package com.soma369.laimory.feature.home.component

import androidx.compose.runtime.Immutable

/**
 * 회전 슬롯이 지금 그릴 한 칸. 값·자리·전체 건수를 **한 덩어리로** 들고 다닌다.
 *
 * 자리만 넘기면 그리는 쪽이 목록을 그 자리로 뒤져야 하는데, 회전 전환은 사라지는 칸을 한
 * 프레임 더 그린다 — 그 사이 목록이 줄면 이미 없는 자리를 뒤져 터진다. 값을 함께 실어
 * 사라지는 칸이 제 값으로 마무리되게 한다.
 */
@Immutable
internal data class HomeRotatingSlot<T>(
    val value: T,
    val index: Int,
    val count: Int,
) {
    /** `1 / 3` 순번 문구. 전체 건수도 이 칸의 것이라 전환 중에 `2 / 1` 같은 짝이 생기지 않는다. */
    val positionLabel: String get() = "${index + 1} / $count"
}

/**
 * [index] 자리의 칸. 비어 있으면 null 이고, 자리가 범위를 벗어나면 가장 가까운 자리로 당긴다.
 *
 * 회전 위치는 목록보다 오래 살아남는다 — 3초마다 올라간 자리를 든 채 수집이 갱신돼 목록이
 * 줄면 그 자리는 이미 없다.
 */
internal fun <T> List<T>.rotatingSlotAt(index: Int): HomeRotatingSlot<T>? {
    if (isEmpty()) return null
    val safeIndex = index.coerceIn(indices)
    return HomeRotatingSlot(value = this[safeIndex], index = safeIndex, count = size)
}
