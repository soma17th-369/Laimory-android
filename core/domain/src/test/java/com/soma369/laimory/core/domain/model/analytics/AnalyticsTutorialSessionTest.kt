package com.soma369.laimory.core.domain.model.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class AnalyticsTutorialSessionTest {
    private val clock = TickingClock()
    private var nextId = 0
    private val session = AnalyticsTutorialSession(AnalyticsTutorial.HOME, clock) { "flow-${++nextId}" }

    @Test
    fun `시작하면 새 회차 토큰으로 시작 이벤트를 만든다`() {
        val begin = session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 7)

        assertEquals(AnalyticsEvent.TutorialBegin("flow-1", AnalyticsTutorial.HOME, AnalyticsTutorialEntryMode.FIRST_RUN, 7), begin)
        assertTrue(session.isActive)
    }

    @Test
    fun `회차가 없으면 아무 이벤트도 만들지 않는다`() {
        assertNull(session.stepViewed(AnalyticsTutorialStep.HOME_DATE))
        assertNull(session.complete())
        assertNull(session.skip(AnalyticsTutorialStep.HOME_DATE, AnalyticsTutorialExitVia.BUTTON))
        assertNull(
            session.dateRecommendationAction(
                AnalyticsTutorialRecommendationAction.SKIP,
                recommendedCount = 3,
                photoAccess = AnalyticsTutorialPhotoAccess.FULL,
            ),
        )
    }

    @Test
    fun `같은 장은 회차마다 한 번만 보인 것으로 남긴다`() {
        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 7)

        val first = session.stepViewed(AnalyticsTutorialStep.HOME_DATE)
        val again = session.stepViewed(AnalyticsTutorialStep.HOME_DATE)

        assertEquals(
            AnalyticsEvent.TutorialStepViewed("flow-1", AnalyticsTutorialEntryMode.FIRST_RUN, AnalyticsTutorialStep.HOME_DATE),
            first,
        )
        assertNull(again)
    }

    @Test
    fun `새 회차에서는 같은 장을 다시 남긴다`() {
        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 7)
        session.stepViewed(AnalyticsTutorialStep.HOME_DATE)
        session.skip(AnalyticsTutorialStep.HOME_DATE, AnalyticsTutorialExitVia.BUTTON)

        session.begin(AnalyticsTutorialEntryMode.REPLAY, stepCount = 6)
        val viewed = session.stepViewed(AnalyticsTutorialStep.HOME_DATE)

        assertEquals("flow-2", viewed?.flowId)
        assertEquals(AnalyticsTutorialEntryMode.REPLAY, viewed?.entryMode)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `다른 튜토리얼의 장은 받지 않는다`() {
        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 7)
        session.stepViewed(AnalyticsTutorialStep.TIMELINE_EVENT)
    }

    @Test
    fun `완료하면 걸린 시간을 싣고 회차를 닫는다`() {
        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 7)
        clock.advance(8_000L)

        val complete = session.complete()

        assertEquals(
            AnalyticsEvent.TutorialComplete("flow-1", AnalyticsTutorial.HOME, AnalyticsTutorialEntryMode.FIRST_RUN, 8_000L),
            complete,
        )
        assertFalse(session.isActive)
        assertNull(session.complete())
    }

    @Test
    fun `건너뛰면 장과 경로와 걸린 시간을 싣고 회차를 닫는다`() {
        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 7)
        clock.advance(1_500L)

        val skip = session.skip(AnalyticsTutorialStep.HOME_DATE_RECOMMENDATION, AnalyticsTutorialExitVia.BACK)

        assertEquals(
            AnalyticsEvent.TutorialSkip(
                "flow-1",
                AnalyticsTutorialEntryMode.FIRST_RUN,
                AnalyticsTutorialStep.HOME_DATE_RECOMMENDATION,
                AnalyticsTutorialExitVia.BACK,
                1_500L,
            ),
            skip,
        )
        assertFalse(session.isActive)
    }

    @Test
    fun `끊긴 회차는 아무것도 보내지 않고 닫는다`() {
        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 4)

        session.abandon()

        assertFalse(session.isActive)
        assertNull(session.complete())
    }

    @Test
    fun `앞에 와 본 카드는 서로 다른 카드만 세고 첫 카드는 처음부터 본 것으로 센다`() {
        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 7)
        session.recommendedCardViewed(1)
        session.recommendedCardViewed(2)
        session.recommendedCardViewed(1)

        val action =
            session.dateRecommendationAction(
                AnalyticsTutorialRecommendationAction.PICK_RECOMMENDED,
                recommendedCount = 3,
                photoAccess = AnalyticsTutorialPhotoAccess.FULL,
                pick = AnalyticsTutorialDatePick(rank = 2, method = AnalyticsTutorialPickMethod.CARD),
            )

        assertEquals(3, action?.cardsViewed)
        assertEquals(2, action?.pick?.rank)
    }

    @Test
    fun `추천이 없으면 본 카드는 0장이다`() {
        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 7)

        val action =
            session.dateRecommendationAction(
                AnalyticsTutorialRecommendationAction.REQUEST_PHOTO_ACCESS,
                recommendedCount = 0,
                photoAccess = AnalyticsTutorialPhotoAccess.NONE,
            )

        assertEquals(0, action?.cardsViewed)
    }

    @Test
    fun `닫힌 회차도 첫 만들기에 한 번만 토큰을 넘긴다`() {
        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 7)
        session.complete()

        assertEquals("flow-1", session.takeFlowIdForCreate())
        assertNull(session.takeFlowIdForCreate())
    }

    @Test
    fun `새 회차를 열면 만들기에 넘길 토큰도 새 회차 것이다`() {
        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 7)
        session.skip(AnalyticsTutorialStep.HOME_DATE, AnalyticsTutorialExitVia.BUTTON)
        session.begin(AnalyticsTutorialEntryMode.REPLAY, stepCount = 6)

        assertEquals("flow-2", session.takeFlowIdForCreate())
    }

    @Test
    fun `회차가 없었거나 끊겼으면 만들기에 넘길 토큰이 없다`() {
        assertNull(session.takeFlowIdForCreate())

        session.begin(AnalyticsTutorialEntryMode.FIRST_RUN, stepCount = 4)
        session.abandon()

        assertNull(session.takeFlowIdForCreate())
    }

    private class TickingClock : Clock() {
        private var now = Instant.parse("2026-11-23T03:00:00Z")

        fun advance(millis: Long) {
            now = now.plusMillis(millis)
        }

        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this
    }
}
