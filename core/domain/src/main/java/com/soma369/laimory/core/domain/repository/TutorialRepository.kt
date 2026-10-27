package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.tutorial.Tutorial
import kotlinx.coroutines.flow.Flow

/** 튜토리얼을 봤는지 기기에 남긴다. 서버와 동기화하지 않고, 로그아웃해도 남는다. */
interface TutorialRepository {
    /** [tutorial] 을 끝냈거나 건너뛰었는지. 읽지 못하면 본 것으로 본다 — 매번 다시 뜨는 것보다 낫다. */
    fun observeSeen(tutorial: Tutorial): Flow<Boolean>

    suspend fun markSeen(tutorial: Tutorial)

    /** 모든 튜토리얼을 보지 않은 상태로 되돌린다. debug 도구만 쓴다. */
    suspend fun resetAll()
}
