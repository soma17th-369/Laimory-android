package com.soma369.laimory.core.domain.model.analytics

import java.time.Clock
import java.util.UUID

/**
 * 튜토리얼 한 회차의 분석 상태. 회차 토큰 · 시작 시각 · 이미 보낸 장 · 앞에 와 본 추천 카드를 들고, 보낼 이벤트를 만든다.
 *
 * 보내지는 않는다 — 돌려준 이벤트를 화면 ViewModel 이 [com.soma369.laimory.core.domain.helper.AnalyticsHelper] 로 보낸다.
 * 회차가 없을 때 온 호출(이미 끝난 뒤 늦게 온 입력)은 null 을 돌려준다. 회차는 메모리에만 있다 — 앱을 끄면 끊긴 회차가 된다.
 *
 * 화면 ViewModel 은 Activity 수명이라 여러 회차를 거친다. [begin] 이 언제나 새 회차를 연다.
 */
class AnalyticsTutorialSession(
    private val tutorial: AnalyticsTutorial,
    private val clock: Clock,
    private val newFlowId: () -> String = { UUID.randomUUID().toString() },
) {
    private var flow: Flow? = null

    /** 회차가 진행 중인지. */
    val isActive: Boolean
        get() = flow != null

    /** 새 회차를 연다. 끝나지 않은 회차가 있으면 끊긴 것으로 버린다. */
    fun begin(
        entryMode: AnalyticsTutorialEntryMode,
        stepCount: Int,
    ): AnalyticsEvent.TutorialBegin {
        val started = Flow(id = newFlowId(), entryMode = entryMode, startedAtMillis = clock.millis())
        flow = started
        return AnalyticsEvent.TutorialBegin(started.id, tutorial, entryMode, stepCount)
    }

    /** [step] 이 보였다. 회차마다 장별로 한 번만 돌려준다 — 구성 변경 · 시트 왕복으로 다시 그려져도 다시 보내지 않는다. */
    fun stepViewed(step: AnalyticsTutorialStep): AnalyticsEvent.TutorialStepViewed? {
        val current = flow ?: return null
        require(step.tutorial == tutorial) { "다른 튜토리얼의 장" }
        if (!current.viewedSteps.add(step)) return null
        return AnalyticsEvent.TutorialStepViewed(current.id, current.entryMode, step)
    }

    /** 추천 날짜 장에서 [index] 번째 카드가 앞에 왔다. */
    fun recommendedCardViewed(index: Int) {
        flow?.viewedCards?.add(index)
    }

    /** 추천 날짜 장에서 [action] 을 골랐다. */
    fun dateRecommendationAction(
        action: AnalyticsTutorialRecommendationAction,
        recommendedCount: Int,
        photoAccess: AnalyticsTutorialPhotoAccess,
        pick: AnalyticsTutorialDatePick? = null,
    ): AnalyticsEvent.TutorialDateRecommendationAction? {
        val current = flow ?: return null
        return AnalyticsEvent.TutorialDateRecommendationAction(
            flowId = current.id,
            entryMode = current.entryMode,
            action = action,
            recommendedCount = recommendedCount,
            photoAccess = photoAccess,
            pick = pick,
            // 카드가 있으면 첫 카드는 처음부터 앞에 있다 — 화면이 알려 오기 전에 골라도 1장은 본 것이다.
            cardsViewed = if (recommendedCount == 0) 0 else (current.viewedCards + 0).size,
        )
    }

    /** 마지막 장에서 `완료`. 회차를 닫는다. */
    fun complete(): AnalyticsEvent.TutorialComplete? {
        val current = close() ?: return null
        return AnalyticsEvent.TutorialComplete(current.id, tutorial, current.entryMode, current.elapsedMillis())
    }

    /** [step] 에서 건너뛰었다. 회차를 닫는다. */
    fun skip(
        step: AnalyticsTutorialStep,
        via: AnalyticsTutorialExitVia,
    ): AnalyticsEvent.TutorialSkip? {
        val current = close() ?: return null
        return AnalyticsEvent.TutorialSkip(current.id, current.entryMode, step, via, current.elapsedMillis())
    }

    /** 완료 · 건너뛰기 없이 회차가 끊겼다(화면을 떠나 코치마크를 거둠). 보내는 것은 없다. */
    fun abandon() {
        flow = null
    }

    private fun close(): Flow? = flow.also { flow = null }

    private fun Flow.elapsedMillis(): Long = (clock.millis() - startedAtMillis).coerceAtLeast(0L)

    private class Flow(
        val id: String,
        val entryMode: AnalyticsTutorialEntryMode,
        val startedAtMillis: Long,
    ) {
        val viewedSteps = mutableSetOf<AnalyticsTutorialStep>()
        val viewedCards = mutableSetOf<Int>()
    }
}
