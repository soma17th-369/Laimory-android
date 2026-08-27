package com.soma369.laimory.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 온보딩 이력의 저장 계약.
 *
 * 완료 여부의 정본은 **서버(계정 단위)** 다. 여기 남는 값은 그 응답의 캐시이며, 계정이 바뀌면
 * 비워야 이전 계정의 완료가 새 계정으로 새지 않는다.
 *
 * 진행 위치(마지막으로 본 장)는 캐시가 아니라 이 설치의 것이다 — 중간에 앱을 닫았다 여는 동안만
 * 쓰이고 계정과 무관하다.
 */
interface OnboardingRepository {
    /** 서버가 준 완료 여부의 캐시. 아직 받은 적이 없으면 `null`. */
    suspend fun cachedCompletion(): Boolean?

    suspend fun cacheCompletion(isCompleted: Boolean)

    /** 서버에 완료를 기록한다. 멱등이라 재시도가 안전하다. */
    suspend fun recordCompletion()

    /** 서버가 보는 완료 여부를 조회한다. */
    suspend fun fetchCompletion(): Result<Boolean>

    fun observeLastPageKey(): Flow<String?>

    suspend fun saveProgress(pageKey: String)

    /** 계정 경계에서 캐시와 진행 위치를 모두 비운다. */
    suspend fun clear()
}
