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

    /**
     * 사용자가 만 14세 이상임을 확인했는지.
     *
     * 서버 약관 catalog 의 항목이 아니라 이 설치가 들고 있는 값이다 — 열람할 원문이 없는 단순
     * 확인이라 서버 단계 정의(`TermStage`)에 끼워 넣으면 그 축이 흐려진다. 나중에 동의 이력이
     * 필요해지면 화면은 그대로 두고 출처만 catalog 로 옮긴다.
     */
    suspend fun isAgeConfirmed(): Boolean

    /**
     * 완료 캐시와 연령 확인을 **한 번의 쓰기**로 남긴다.
     *
     * 연령 확인 없이는 온보딩을 끝낼 수 없으므로 둘은 항상 같이 참이 된다. 따로 쓰면 그 사이에
     * 앱이 죽었을 때 **확인 없이 완료된 상태**가 남고, 나중에 연령 확인을 루트 게이트로 쓰려 할 때
     * 그 사용자만 판정이 갈린다.
     */
    suspend fun cacheCompletionWithAgeConfirmation()

    /** 서버에 완료를 기록한다. 멱등이라 재시도가 안전하다. */
    suspend fun recordCompletion()

    /**
     * 완료했지만 아직 서버에 올리지 못했는지.
     *
     * 완료 여부의 정본이 서버라, 기록이 빠지면 다음 실행에서 서버가 `false` 를 주고 사용자가
     * 끝낸 온보딩을 다시 본다. 올릴 때까지 이 표시를 남겨 두고 로컬을 믿는다.
     */
    suspend fun isCompletionPending(): Boolean

    suspend fun setCompletionPending(isPending: Boolean)

    /** 서버가 보는 완료 여부를 조회한다. */
    suspend fun fetchCompletion(): Result<Boolean>

    fun observeLastPageKey(): Flow<String?>

    suspend fun saveProgress(pageKey: String)

    /** 계정 경계에서 캐시와 진행 위치를 모두 비운다. */
    suspend fun clear()
}
