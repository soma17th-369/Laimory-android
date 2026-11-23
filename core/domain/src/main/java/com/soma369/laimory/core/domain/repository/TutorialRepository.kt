package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.tutorial.Tutorial
import kotlinx.coroutines.flow.Flow

/** 튜토리얼을 봤는지 기기에 남긴다. 서버와 동기화하지 않고, 로그아웃해도 남는다. */
interface TutorialRepository {
    /** [tutorial] 을 끝냈거나 건너뛰었는지. 읽지 못하면 본 것으로 본다 — 매번 다시 뜨는 것보다 낫다. */
    fun observeSeen(tutorial: Tutorial): Flow<Boolean>

    /** [tutorial] 을 끝냈거나 건너뛰었다. 다시 시작한 표시([restartAll])도 걷는다. */
    suspend fun markSeen(tutorial: Tutorial)

    /** 모든 튜토리얼을 보지 않은 상태로 되돌린다. 처음 보는 것과 같아진다 — debug 도구만 쓴다. */
    suspend fun resetAll()

    /**
     * 사용자가 설정 `처음부터 따라 하기` 로 모든 튜토리얼을 다시 시작했다. 보지 않은 상태로 되돌리되, 다시 시작했다고 표시해
     * 자동으로 뜨는 회차를 처음 보는 것과 나눈다. 표시는 그 튜토리얼을 끝내거나 건너뛸 때([markSeen]) 걷힌다.
     */
    suspend fun restartAll()

    /** [tutorial] 이 [restartAll] 뒤 아직 끝나지 않았는지. 읽지 못하면 아니라고 본다. */
    suspend fun isRestarted(tutorial: Tutorial): Boolean
}
