package com.soma369.laimory.core.collection.collector

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 종일 일정 시각을 실제 시각으로 바꾸는 규칙.
 *
 * Android 캘린더는 종일 일정의 BEGIN/END 를 실제 시각이 아니라 "UTC 자정"으로 적은 날짜 표기로 둔다
 * (15일 종일 → 15일 00:00 UTC ~ 16일 00:00 UTC). 이를 그대로 실제 시각으로 쓰면 KST 기준 09:00 에
 * 시작하는 일정이 돼 다음 날 기록 창(06:00 ~)과 겹친다(#504).
 *
 * 그래서 UTC 기준 날짜만 꺼낸 뒤 [zone] 자정으로 다시 잡는다(15일 00:00 KST ~ 16일 00:00 KST).
 * 그 뒤로는 시간 지정 일정과 같은 겹침 규칙으로 기록 창 포함 여부를 정한다.
 *
 * 수집([CalendarCollector])과 기존 저장분 정정(DB 마이그레이션)이 같은 규칙을 쓰도록 한 곳에 둔다.
 */
internal object CalendarAllDayPolicy {
    /** 종일 일정의 BEGIN/END 값 [utcDateMillis] 가 가리키는 날짜의 [zone] 자정. */
    fun toZonedMidnight(
        utcDateMillis: Long,
        zone: ZoneId,
    ): Instant =
        Instant
            .ofEpochMilli(utcDateMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
}
