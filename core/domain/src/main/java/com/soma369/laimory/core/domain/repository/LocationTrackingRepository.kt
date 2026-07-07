package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import kotlinx.coroutines.flow.Flow

/**
 * 위치 자동 수집(추적) 토글의 구동 계약. 구현은 `:core:collection` 이 소유한다.
 *
 * Phase 1 은 foreground 한정이라 토글은 세션 상태로만 다루며 영속 저장하지 않는다 — 콜드 스타트마다 off 로
 * 시작한다. 백그라운드 지속·부팅 복원은 Phase 2 몫이다.
 */
interface LocationTrackingRepository {
    /** 현재 추적 활성 여부를 관찰한다(세션 한정, 콜드 스타트 시 off). */
    fun observeEnabled(): Flow<Boolean>

    /** 진행 중 세그먼트의 라이브 상태(체류 중/이동 중)를 관찰한다. 추적 중이 아니면 null. */
    fun observeStatus(): Flow<LocationTrackingStatus?>

    /** 추적을 켜거나 끈다(위치 업데이트 시작/중지). */
    suspend fun setEnabled(enabled: Boolean)
}
