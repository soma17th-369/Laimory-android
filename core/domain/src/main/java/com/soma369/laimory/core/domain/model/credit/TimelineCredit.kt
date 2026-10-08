package com.soma369.laimory.core.domain.model.credit

/**
 * 타임라인을 만들기 직전의 크레딧 — 이번에 쓸 비용과 지금 남은 양.
 *
 * 남은 양은 만들기를 누른 시점의 값이다. 차감은 요청이 아니라 결과가 저장될 때 일어나므로, 앞선 생성이 끝나기 전에는
 * 아직 줄지 않은 값일 수 있다(서버의 사전 검사도 같은 값을 본다).
 */
data class TimelineCredit(
    val cost: Int,
    val remaining: Int,
) {
    /** 이번 만들기에 쓸 만큼 남았는지. 서버는 `remaining < cost` 면 403 `-1021` 로 거절한다. */
    val isEnough: Boolean get() = remaining >= cost
}
