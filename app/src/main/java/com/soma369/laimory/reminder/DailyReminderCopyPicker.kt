package com.soma369.laimory.reminder

import kotlin.random.Random

/**
 * 알림 문구를 고른다.
 *
 * 고르는 시점은 예약할 때가 아니라 띄우는 순간이다 — 그래야 날마다 달라진다. 바로 전에 쓴 제목(완료 문구는
 * 짝)은 피한다. 후보가 하나뿐이면 그대로 쓴다.
 */
internal class DailyReminderCopyPicker(
    private val random: Random = Random.Default,
) {
    fun pickGeneral(
        titleCount: Int,
        bodyCount: Int,
        lastTitleIndex: Int?,
    ): DailyReminderCopy.General {
        val titleIndex = pickAvoiding(titleCount, lastTitleIndex)
        val bodies = (0 until bodyCount).filterNot { body -> (titleIndex to body) in EXCLUDED_GENERAL_PAIRS }
        return DailyReminderCopy.General(titleIndex = titleIndex, bodyIndex = bodies.random(random))
    }

    fun pickCompleted(
        count: Int,
        lastIndex: Int?,
    ): DailyReminderCopy.Completed = DailyReminderCopy.Completed(pickAvoiding(count, lastIndex))

    private fun pickAvoiding(
        count: Int,
        last: Int?,
    ): Int {
        val candidates = (0 until count).filterNot { index -> index == last }.ifEmpty { (0 until count).toList() }
        return candidates.random(random)
    }

    companion object {
        /**
         * 같이 쓰지 않는 (제목, 본문) 조합.
         *
         * `오늘 하루 어땠나요?` 와 `오늘 하루를 타임라인으로 정리해보세요.` 는 "오늘 하루"가 두 번 나온다.
         */
        val EXCLUDED_GENERAL_PAIRS: Set<Pair<Int, Int>> = setOf(0 to 1)
    }
}
