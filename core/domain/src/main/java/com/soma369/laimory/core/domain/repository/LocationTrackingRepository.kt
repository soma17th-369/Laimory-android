package com.soma369.laimory.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 위치 자동 수집(추적) 토글의 로컬 저장·구동 계약. 구현은 `:core:collection` 이 소유한다.
 *
 * [setEnabled] 는 토글 상태를 저장하면서 실제 위치 업데이트 수신을 시작/중지한다(foreground 한정, Phase 1).
 */
interface LocationTrackingRepository {
    /** 현재 추적 활성 여부를 관찰한다. */
    fun observeEnabled(): Flow<Boolean>

    /** 추적을 켜거나 끈다(상태 저장 + 위치 업데이트 시작/중지). */
    suspend fun setEnabled(enabled: Boolean)
}
