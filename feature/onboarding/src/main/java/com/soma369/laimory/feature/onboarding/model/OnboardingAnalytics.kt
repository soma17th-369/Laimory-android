package com.soma369.laimory.feature.onboarding.model

import com.soma369.laimory.core.domain.model.analytics.AnalyticsOnboardingEligibility
import com.soma369.laimory.core.domain.model.analytics.AnalyticsOnboardingStep
import com.soma369.laimory.core.domain.model.analytics.AnalyticsOnboardingVersion
import com.soma369.laimory.core.ui.permission.DataPermissionAction
import com.soma369.laimory.core.ui.permission.DataPermissionState
import com.soma369.laimory.core.ui.permission.DataSourceStatus

/** [ONBOARDING_PAGES] 의 구성 판. 장의 순서나 구성을 바꾸면 새 판으로 올린다. */
internal val ONBOARDING_VERSION = AnalyticsOnboardingVersion.V1

/**
 * 장 키를 분석용 장으로 옮긴다. 목록에 없는 키(구성에서 빠진 장)는 null 이라 기록하지 않는다.
 *
 * 키는 진행 복원에도 쓰이는 화면 쪽 값이라, 전송값은 [AnalyticsOnboardingStep] 으로 따로 고정한다.
 */
internal val OnboardingPageSpec.analyticsStep: AnalyticsOnboardingStep?
    get() =
        when (key) {
            "intro" -> AnalyticsOnboardingStep.INTRO
            "photo" -> AnalyticsOnboardingStep.PHOTO
            "calendar" -> AnalyticsOnboardingStep.CALENDAR
            "location" -> AnalyticsOnboardingStep.LOCATION
            "notification" -> AnalyticsOnboardingStep.NOTIFICATION
            "app_notification" -> AnalyticsOnboardingStep.APP_NOTIFICATION
            "done" -> AnalyticsOnboardingStep.DONE
            else -> null
        }

/**
 * 장이 보인 순간의 권한 상태.
 *
 * 이 장을 끝낼 수 있는지([isPageDone])를 먼저 본다 — 위치는 `사용 중에만` 허용이면 아직 `항상 허용` 을
 * 요청할 수 있어 요청 필요다. 설정으로만 갈 수 있는 것은 알림 접근처럼 다이얼로그가 없거나, 두 번 거부해
 * 시스템이 더 묻지 않는 경우다.
 */
internal fun DataPermissionState.eligibilityOf(page: OnboardingPageSpec): AnalyticsOnboardingEligibility {
    val permission = page.permission ?: return AnalyticsOnboardingEligibility.NOT_APPLICABLE
    if (isPageDone(permission)) return AnalyticsOnboardingEligibility.ALREADY_USABLE
    if (statusOf(permission) == DataSourceStatus.UNSUPPORTED) return AnalyticsOnboardingEligibility.NOT_SUPPORTED
    return when (actionFor(permission)) {
        DataPermissionAction.APP_SETTINGS,
        DataPermissionAction.LISTENER_SETTINGS,
        DataPermissionAction.HEALTH_SETTINGS,
        -> AnalyticsOnboardingEligibility.SETTINGS_ONLY
        DataPermissionAction.REQUEST,
        DataPermissionAction.RESELECT_PHOTOS,
        -> AnalyticsOnboardingEligibility.NEEDS_REQUEST
        DataPermissionAction.NONE -> AnalyticsOnboardingEligibility.NOT_SUPPORTED
    }
}
