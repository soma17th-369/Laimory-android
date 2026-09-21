package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.analytics.AnalyticsConsent
import kotlinx.coroutines.flow.Flow

/**
 * 제품 분석 수집 동의를 설치 단위로 보관한다.
 *
 * 계정이 아니라 설치 단위다. GA4 수집이 앱 인스턴스 단위라 계정과 맞추면 어긋나고, 로그아웃이
 * 비우는 자리에 두면 다시 로그인할 때마다 동의가 사라진다.
 */
interface AnalyticsConsentRepository {
    val consent: Flow<AnalyticsConsent>

    suspend fun set(consent: AnalyticsConsent)
}
