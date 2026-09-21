package com.soma369.laimory.core.ui.permission

import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType

/**
 * 요청하는 순간의 분석용 권한 종류.
 *
 * 위치는 단계마다 다른 권한이라 **지금 넘으려는 단계**로 가른다 — 전경을 받은 뒤의 요청은 백그라운드
 * 요청이다. 모든 단계를 받아 더 요청할 것이 없으면 null.
 */
internal fun DataPermission.analyticsTypeAt(locationStep: LocationPermissionStep): AnalyticsPermissionType? =
    when (this) {
        DataPermission.PHOTO -> AnalyticsPermissionType.PHOTO
        DataPermission.CALENDAR -> AnalyticsPermissionType.CALENDAR
        DataPermission.NOTIFICATION_LISTENER -> AnalyticsPermissionType.NOTIFICATION_LISTENER
        DataPermission.APP_NOTIFICATION -> AnalyticsPermissionType.PUSH_NOTIFICATION
        DataPermission.HEALTH -> AnalyticsPermissionType.HEALTH_CONNECT
        DataPermission.LOCATION ->
            when (locationStep) {
                LocationPermissionStep.FOREGROUND -> AnalyticsPermissionType.LOCATION_FOREGROUND
                LocationPermissionStep.BACKGROUND -> AnalyticsPermissionType.LOCATION_BACKGROUND
                LocationPermissionStep.ACTIVITY -> AnalyticsPermissionType.ACTIVITY_RECOGNITION
                LocationPermissionStep.GRANTED -> null
            }
    }
