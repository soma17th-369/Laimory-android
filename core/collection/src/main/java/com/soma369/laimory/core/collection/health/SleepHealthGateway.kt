package com.soma369.laimory.core.collection.health

import java.time.Instant

/**
 * Health Connect 수면 세션 저수준 접근 seam.
 *
 * 실제 구현은 [HealthConnectSleepGateway](HealthConnectClient), 테스트는 인메모리 페이크로 대체해
 * 정책([SleepHealthRecorder])을 순수 JVM 에서 검증한다.
 */
internal interface SleepHealthGateway {
    /** 수면 세션을 쓴다. 같은 [SleepWrite.clientRecordId] 는 덮어쓴다(멱등 upsert). */
    suspend fun upsert(session: SleepWrite)

    /** 해당 clientRecordId 로 우리가 쓴 세션을 지운다. */
    suspend fun deleteByClientRecordId(clientRecordId: String)

    /** `[start, end)` 구간과 겹치는 수면 세션을 읽는다. */
    suspend fun read(
        start: Instant,
        end: Instant,
    ): List<StoredSleepSession>
}
