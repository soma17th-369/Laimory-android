package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** 화면과 독립적으로 현재 초안 작업의 polling·복구·결과 저장을 조정한다. */
interface DraftTaskCoordinator {
    val state: StateFlow<DraftTaskTrackingState>

    suspend fun start(
        taskId: String,
        recordDate: LocalDate,
    )

    suspend fun onForeground()

    suspend fun onBackground()

    fun retry()

    fun continueWaiting()

    suspend fun discard()
}
