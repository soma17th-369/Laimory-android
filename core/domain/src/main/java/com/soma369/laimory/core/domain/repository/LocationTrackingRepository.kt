package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import kotlinx.coroutines.flow.Flow

/**
 * 위치 자동 수집의 구동 계약. 구현은 `:core:collection` 이 소유한다.
 *
 * 수집은 **상태**다 — "권한이 있고 사용자가 끄지 않았으면 켜져 있다". 한 번 지나가는 화면이
 * 켜는 것이 아니라 앱이 전경으로 올 때마다 [reconcile] 이 그 상태를 맞춘다. 지나가는 이벤트에
 * 매달아 두면 그 이벤트가 없는 기기(재설치·기기 교체로 온보딩을 보지 않는 계정)에서는 수집이
 * 영영 시작되지 않는다.
 */
interface LocationTrackingRepository {
    /** 사용자가 수집을 원하는지. 끄기 전까지 참이며, 실패로 멈춘 것은 이 값을 바꾸지 않는다. */
    fun observeEnabled(): Flow<Boolean>

    /** 진행 중 세그먼트의 라이브 상태(체류 중/이동 중)를 관찰한다. 수집 중이 아니면 null. */
    fun observeStatus(): Flow<LocationTrackingStatus?>

    /** 사용자가 직접 켜거나 끈다. 사용자가 끈 것은 [reconcile] 이 되살리지 않는다. */
    suspend fun setEnabled(enabled: Boolean)

    /**
     * 지금 상태에 맞게 수집을 켠다. 사용자가 껐거나 권한이 없으면 아무것도 하지 않는다.
     *
     * 이미 돌고 있어도 무해하다 — 같은 서비스를 다시 시작할 뿐이고 진행 중 세그먼트는 유지된다.
     */
    suspend fun reconcile()
}
