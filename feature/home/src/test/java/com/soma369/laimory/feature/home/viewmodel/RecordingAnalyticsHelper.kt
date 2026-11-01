package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKey
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.InstallAttribution

/** 보낸 이벤트를 차례대로 모아 두는 가짜. 중복 방지 판정은 하지 않고 [logOnce] 도 그대로 모은다. */
class RecordingAnalyticsHelper : AnalyticsHelper {
    val logged = mutableListOf<AnalyticsEvent>()

    override suspend fun log(event: AnalyticsEvent) {
        logged += event
    }

    override suspend fun logOnce(
        key: AnalyticsDedupeKey,
        event: AnalyticsEvent,
    ) {
        logged += event
    }

    override suspend fun forgetOnce(key: AnalyticsDedupeKey) = Unit

    override fun setUserId(userId: Long?) = Unit

    override fun setInstallAttribution(attribution: InstallAttribution) = Unit
}
