package com.soma369.laimory.core.ui.permission

import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionState
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType

/**
 * 권한 요청이 실제로 일어난 순간과 그 결과.
 *
 * 화면은 이것을 자기 ViewModel 로 넘기고, ViewModel 이 분석에 기록한다. 권한 상태는 컴포저블이라
 * 분석을 직접 주입받지 않는다.
 */
sealed interface DataPermissionEvent {
    /** 런타임 다이얼로그·Health Connect·설정 화면을 **띄우기 직전**. 이미 허용된 것을 바꾸러 가는 경우는 빠진다. */
    data class Requested(
        val permission: AnalyticsPermissionType,
    ) : DataPermissionEvent

    /** 돌아와서 권한을 **다시 읽은** 상태. 콜백 결과가 아니라 실제 상태로 귀속한다. */
    data class Settled(
        val permission: AnalyticsPermissionType,
        val state: AnalyticsPermissionState,
    ) : DataPermissionEvent
}
