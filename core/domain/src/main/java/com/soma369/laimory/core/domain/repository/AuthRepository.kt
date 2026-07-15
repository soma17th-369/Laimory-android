package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import kotlinx.coroutines.flow.Flow

/** 소셜 로그인 토큰 교환과 이 기기의 인증 세션을 관리하는 도메인 계약. */
interface AuthRepository {
    /** 로컬 토큰 저장 여부를 인증 상태로 변환하며, 저장소 확인 전에는 [AuthSessionState.Loading]을 방출한다. */
    fun observeSessionState(): Flow<AuthSessionState>

    /** 로그인 callback의 일회용 code를 verifier와 교환하고 발급된 토큰 쌍을 하나의 세션으로 저장한다. */
    suspend fun issueTokens(
        appCode: String,
        appVerifier: String,
    )

    /** 서버 refresh 폐기를 시도한 뒤 결과와 무관하게 이 기기의 세션을 제거한다. */
    suspend fun logout()
}
