package com.soma369.laimory.reminder

/**
 * 알림에 쓸 문구의 자리. 실제 글자는 리소스 배열에 있다.
 *
 * 제목과 본문은 서로 어울리게 쓴 짝이라 [index] 하나로 함께 고른다.
 */
internal sealed interface DailyReminderCopy {
    val index: Int

    /** 작성을 권하는 문구. */
    data class General(
        override val index: Int,
    ) : DailyReminderCopy

    /** 오늘 기록을 이미 마친 날의 문구. */
    data class Completed(
        override val index: Int,
    ) : DailyReminderCopy
}
