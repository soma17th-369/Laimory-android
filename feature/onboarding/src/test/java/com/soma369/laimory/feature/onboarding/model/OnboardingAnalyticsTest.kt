package com.soma369.laimory.feature.onboarding.model

import com.soma369.laimory.core.domain.model.analytics.AnalyticsOnboardingEligibility
import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.DataPermissionState
import com.soma369.laimory.core.ui.permission.LocationPermissionStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OnboardingAnalyticsTest {
    private fun page(key: String) = ONBOARDING_PAGES.first { it.key == key }

    private fun state(
        granted: Set<DataPermission> = emptySet(),
        locationStep: LocationPermissionStep = LocationPermissionStep.FOREGROUND,
        hasListenerSettings: Boolean = true,
        blocked: Set<DataPermission> = emptySet(),
    ) = DataPermissionState(
        granted = granted,
        locationStep = locationStep,
        isPhotoLimited = false,
        hasListenerSettings = hasListenerSettings,
        blocked = blocked,
        onRequest = {},
    )

    @Test
    fun `모든 장이 분석용 장을 갖는다`() {
        ONBOARDING_PAGES.forEach { assertNotNull(it.key, it.analyticsStep) }
    }

    @Test
    fun `안내 장은 권한과 무관하다`() {
        assertEquals(AnalyticsOnboardingEligibility.NOT_APPLICABLE, state().eligibilityOf(page("intro")))
        assertEquals(AnalyticsOnboardingEligibility.NOT_APPLICABLE, state().eligibilityOf(page("done")))
    }

    @Test
    fun `받지 않은 권한은 요청 필요, 받은 권한은 이미 사용 가능이다`() {
        assertEquals(AnalyticsOnboardingEligibility.NEEDS_REQUEST, state().eligibilityOf(page("photo")))
        assertEquals(
            AnalyticsOnboardingEligibility.ALREADY_USABLE,
            state(granted = setOf(DataPermission.PHOTO)).eligibilityOf(page("photo")),
        )
    }

    @Test
    fun `위치를 사용 중에만 허용했으면 항상 허용이 남아 요청 필요다`() {
        assertEquals(
            AnalyticsOnboardingEligibility.NEEDS_REQUEST,
            state(locationStep = LocationPermissionStep.BACKGROUND).eligibilityOf(page("location")),
        )
        assertEquals(
            AnalyticsOnboardingEligibility.ALREADY_USABLE,
            state(locationStep = LocationPermissionStep.GRANTED).eligibilityOf(page("location")),
        )
    }

    @Test
    fun `다이얼로그 없이 설정으로만 켤 수 있으면 설정 전용이다`() {
        assertEquals(AnalyticsOnboardingEligibility.SETTINGS_ONLY, state().eligibilityOf(page("notification")))
        assertEquals(
            AnalyticsOnboardingEligibility.SETTINGS_ONLY,
            state(blocked = setOf(DataPermission.CALENDAR)).eligibilityOf(page("calendar")),
        )
    }

    @Test
    fun `켤 방법이 없는 기기면 미지원이다`() {
        assertEquals(
            AnalyticsOnboardingEligibility.NOT_SUPPORTED,
            state(hasListenerSettings = false).eligibilityOf(page("notification")),
        )
    }
}
