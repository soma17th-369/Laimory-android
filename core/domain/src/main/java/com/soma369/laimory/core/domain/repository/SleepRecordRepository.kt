package com.soma369.laimory.core.domain.repository

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 수면을 Health Connect 에 기록/조회하는 계약(에픽 #142). 구현은 `:core:collection` 이 소유한다.
 *
 * Laimory 는 HC 프로듀서라 자체 저장 없이 HC 에 써넣기만 한다 — 수집(read)은 기존 HC-read 경로가 픽업한다.
 */
interface SleepRecordRepository {
    /** `[start, end)` 창에 (우리·외부 무관) 수면 세션이 이미 있는지. 사용자 입력 유도 여부 판단에 쓴다. */
    suspend fun hasSleep(
        start: Instant,
        end: Instant,
    ): Boolean

    /** 사용자가 입력한 수면을 HC 에 기록한다(manualEntry). 같은 [night] 이면 새 세션 없이 갱신된다. */
    suspend fun recordManualSleep(
        night: LocalDate,
        start: Instant,
        end: Instant,
        zoneOffset: ZoneOffset?,
    )
}
