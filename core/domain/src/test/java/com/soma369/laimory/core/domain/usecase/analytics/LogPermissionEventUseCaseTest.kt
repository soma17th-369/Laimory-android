package com.soma369.laimory.core.domain.usecase.analytics

import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKey
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKeys
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionState
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPermissionType
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPromptContext
import com.soma369.laimory.core.domain.model.analytics.AnalyticsReadyTrigger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogPermissionEventUseCaseTest {
    private val helper = RecordingAnalyticsHelper()
    private val useCase = LogPermissionEventUseCase(helper)

    @Test
    fun `요청을 기록한다`() =
        runTest {
            useCase.requested(AnalyticsPermissionType.CALENDAR, AnalyticsPromptContext.SETTINGS)

            assertEquals(
                listOf(AnalyticsEvent.PermissionRequestStarted(AnalyticsPermissionType.CALENDAR, AnalyticsPromptContext.SETTINGS)),
                helper.logged,
            )
        }

    @Test
    fun `사진 권한이 허용되면 생성 재료가 준비된 것으로 한 번 기록한다`() =
        runTest {
            useCase.settled(AnalyticsPermissionType.PHOTO, AnalyticsPermissionState.GRANTED, AnalyticsPromptContext.HOME)

            assertEquals(
                listOf(
                    AnalyticsDedupeKeys.DATA_COLLECTION_READY to AnalyticsEvent.DataCollectionReady(AnalyticsReadyTrigger.PHOTO_PERMISSION),
                ),
                helper.loggedOnce,
            )
        }

    @Test
    fun `사진 일부 선택도 준비된 것으로 본다`() =
        runTest {
            useCase.settled(AnalyticsPermissionType.PHOTO, AnalyticsPermissionState.PARTIAL, AnalyticsPromptContext.APP_START)

            assertEquals(1, helper.loggedOnce.size)
        }

    @Test
    fun `사진 거부나 다른 권한 허용은 준비로 보지 않는다`() =
        runTest {
            useCase.settled(AnalyticsPermissionType.PHOTO, AnalyticsPermissionState.DENIED, AnalyticsPromptContext.HOME)
            useCase.settled(AnalyticsPermissionType.CALENDAR, AnalyticsPermissionState.GRANTED, AnalyticsPromptContext.HOME)

            assertTrue(helper.loggedOnce.isEmpty())
            assertEquals(2, helper.logged.size)
        }

    private class RecordingAnalyticsHelper : AnalyticsHelper {
        val logged = mutableListOf<AnalyticsEvent>()
        val loggedOnce = mutableListOf<Pair<AnalyticsDedupeKey, AnalyticsEvent>>()

        override suspend fun log(event: AnalyticsEvent) {
            logged += event
        }

        override suspend fun logOnce(
            key: AnalyticsDedupeKey,
            event: AnalyticsEvent,
        ) {
            loggedOnce += key to event
        }

        override fun setUserId(userId: Long?) = Unit
    }
}
