package com.soma369.laimory.core.domain.helper

import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 전체 입력을 차단하는 전역 로딩의 key 기반 상태 포트.
 *
 * 인증 초기화·계정 전환처럼 앱 전역 조작을 막아야 하는 작업에만 쓰고,
 * 현재 화면만 제한하면 되는 부분 로딩은 각 ViewModel의 UiState로 유지한다.
 * 메시지 상태([MessageHelper])와는 분리된 별도 상태다.
 */
interface GlobalLoadingHelper {
    /** 현재 로딩 중인 작업 key 집합. 비어 있지 않으면 Root가 전역 로딩을 표시한다. */
    val activeKeys: StateFlow<Set<String>>

    /**
     * [block] 실행 동안 [key]를 로딩 상태로 유지한다.
     *
     * 같은 key가 겹쳐도 모든 실행이 끝날 때까지 로딩이 유지되며, 취소·예외에도 key가 해제된다.
     */
    suspend fun <T> withLoading(
        key: String,
        block: suspend () -> T,
    ): T
}
