package com.soma369.laimory.reminder

import kotlin.random.Random

/**
 * 알림 문구를 고른다.
 *
 * 문구는 제목·본문 짝 단위로 고른다 — 서로 어울리게 쓴 한 벌이라 섞지 않는다. 고르는 시점은 예약할 때가 아니라
 * 띄우는 순간이다 — 그래야 날마다 달라진다. 바로 전에 쓴 짝은 피한다. 후보가 하나뿐이면 그대로 쓴다.
 */
internal class DailyReminderCopyPicker(
    private val random: Random = Random.Default,
) {
    fun pickGeneral(
        count: Int,
        lastIndex: Int?,
    ): DailyReminderCopy.General = DailyReminderCopy.General(pickAvoiding(count, lastIndex))

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
}
