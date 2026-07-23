package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import kotlinx.coroutines.flow.Flow

/** 프로세스 재생성 뒤에도 서버 초안 작업을 재조회하기 위한 최소 로컬 저장 계약. */
interface ActiveDraftTaskRepository {
    fun observe(): Flow<ActiveDraftTask?>

    suspend fun get(): ActiveDraftTask?

    suspend fun save(task: ActiveDraftTask)

    suspend fun clear()
}
