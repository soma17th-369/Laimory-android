package com.soma369.laimory.feature.home.state

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 사용자가 날짜를 고르지 않았을 때 홈이 골라 두는 기록 날짜.
 *
 * 하루 타임라인은 최소 6시간쯤 쌓여야 만들 의미가 있다. [TODAY_FROM] 전에는 오늘 모인 것이 거의 없고
 * 어제를 마무리할 때라 어제를 둔다.
 *
 * **날짜의 정의는 바꾸지 않는다.** 기록 창(`00:00 ~ 익일 00:00`), 피커의 오늘·미래 판정, CTA 의
 * `오늘`·`어제` 는 달력 날짜 그대로다. 여기서 정하는 것은 처음 골라 두는 값뿐이다.
 */
object HomeDefaultDate {
    /** 이 시각부터 오늘을 기본으로 둔다. */
    val TODAY_FROM: LocalTime = LocalTime.of(6, 0)

    fun of(now: LocalDateTime): LocalDate =
        if (now.toLocalTime().isBefore(TODAY_FROM)) now.toLocalDate().minusDays(1) else now.toLocalDate()

    /**
     * 기본 날짜나 달력상 오늘이 다음으로 바뀌는 시각.
     *
     * 자정에는 기본 날짜가 그대로지만 CTA 의 `오늘`·`어제` 가 바뀌므로 둘 중 이른 쪽을 준다.
     */
    fun nextChangeAfter(now: LocalDateTime): LocalDateTime {
        val todayFrom = now.toLocalDate().atTime(TODAY_FROM)
        return if (now.isBefore(todayFrom)) todayFrom else now.toLocalDate().plusDays(1).atStartOfDay()
    }
}
