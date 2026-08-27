package com.soma369.laimory.core.domain.coordinator

import kotlinx.coroutines.flow.StateFlow

/**
 * 인증 세션 하나당 온보딩 완료 여부를 한 번만 조회해 앱 루트가 나눠 쓰게 한다.
 *
 * 완료 여부는 **계정 단위**다. 같은 계정으로 다시 로그인하면 기기를 바꿔도 온보딩을 다시 보지
 * 않고, 다른 계정으로 들어오면 그 계정 기준으로 다시 판정한다.
 */
interface OnboardingCompletionCoordinator {
    /**
     * 현재 세션의 완료 여부. `null` 이면 **아직 모른다** 는 뜻이다.
     *
     * 앱 루트는 이 값이 정해질 때까지 Home 도 온보딩도 열지 않는다 — 모르는 채로 하나를 고르면
     * 반대였을 때 화면이 한 번 번쩍인 뒤 갈린다.
     */
    val completed: StateFlow<Boolean?>

    /** 아직 조회하지 못했으면 다시 시도한다. */
    fun refresh()

    /** 완료를 확정한다. 서버 기록과 캐시를 함께 갱신한다. */
    suspend fun markCompleted()

    /**
     * 이 세션에 한해 완료를 되돌린다. QA 반복 확인용이다.
     *
     * 서버에는 `false` 로 되돌리는 API 가 없다. 그래서 다시 조회하지 않고 **현재 세션의 값만**
     * 내린다 — 다시 조회하면 서버가 곧장 완료라고 답해 온보딩이 열리자마자 닫힌다.
     */
    suspend fun resetForCurrentSession()
}
