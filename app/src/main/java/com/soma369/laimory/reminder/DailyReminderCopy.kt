package com.soma369.laimory.reminder

/** 알림에 쓸 문구의 자리. 실제 글자는 리소스 배열에 있다. */
internal sealed interface DailyReminderCopy {
    /** 작성을 권하는 문구. 제목과 본문을 따로 고른다. */
    data class General(
        val titleIndex: Int,
        val bodyIndex: Int,
    ) : DailyReminderCopy

    /** 오늘 기록을 이미 마친 날의 문구. 제목과 본문이 짝이다. */
    data class Completed(
        val index: Int,
    ) : DailyReminderCopy
}
