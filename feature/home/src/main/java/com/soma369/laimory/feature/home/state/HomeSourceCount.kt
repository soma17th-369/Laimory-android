package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/**
 * 원천 카드 본문 `M개 중 N개`.
 *
 * [candidate] 는 기록 창 안에 모인 후보 수, [sending] 은 실제로 전송될 수다. 둘이 갈리는 이유는
 * 타입별 상한과 사용자 제외다 — 사진만은 자동 절삭이 없어 [sending] 이 곧 고른 수다.
 */
@Immutable
data class HomeSourceCount(
    val candidate: Int = 0,
    val sending: Int = 0,
)
