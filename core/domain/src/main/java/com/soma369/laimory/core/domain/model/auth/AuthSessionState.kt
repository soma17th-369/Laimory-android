package com.soma369.laimory.core.domain.model.auth

/** 앱 시작 게이트와 런타임 세션 만료 전환이 관찰하는 인증 상태. */
sealed interface AuthSessionState {
    /** 암호화 저장소의 최초 값을 아직 확인하지 못한 상태. */
    data object Loading : AuthSessionState

    /** 사용할 수 있는 로컬 access/refresh token 쌍이 존재하는 상태. */
    data object Authenticated : AuthSessionState

    /** 로컬 인증 세션이 없거나 복호화할 수 없는 상태. */
    data object Unauthenticated : AuthSessionState
}
