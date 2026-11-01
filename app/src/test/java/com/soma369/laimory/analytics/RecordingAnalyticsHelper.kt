package com.soma369.laimory.analytics

import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKey
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.InstallAttribution

/** 보낸 이벤트를 차례대로 모아 두는 가짜. 전송 형태는 매퍼 테스트가 보므로 여기서는 도메인 이벤트만 본다. */
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
