package com.soma369.laimory.core.collection.health

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 수면을 Health Connect 에 기록하는 프로듀서 정책(에픽 #142).
 *
 * 밤(날짜)당 clientRecordId 하나를 부여해 같은 밤 재기록이 중복 세션을 만들지 않게 한다.
 * 정합성 판정([hasExternalSleep])은 우리 앱([selfPackageName])이 아닌 dataOrigin 세션을 외부로 본다 —
 * "그 밤에 외부 수면 기록이 있으면 우리는 쓰지 않는다" 규칙의 판단 재료다.
 * 실제로 쓸지 말지(dedup) 결정은 호출자(Sleep API 오케스트레이션, #144)가 [hasExternalSleep] 로 판단한다.
 */
internal class SleepHealthRecorder(
    private val gateway: SleepHealthGateway,
    private val selfPackageName: String,
) {
    /** 감지된 수면을 기록한다(autoRecorded). 같은 밤이면 새 세션 없이 갱신된다. */
    suspend fun recordDetectedSleep(
        night: LocalDate,
        start: Instant,
        end: Instant,
        zoneOffset: ZoneOffset?,
    ) = upsert(night, start, end, zoneOffset, SleepRecordingMethod.AUTO_DETECTED)

    /** 사용자가 입력한 수면을 기록한다(manualEntry). 같은 밤이면 새 세션 없이 갱신된다. */
    suspend fun recordManualSleep(
        night: LocalDate,
        start: Instant,
        end: Instant,
        zoneOffset: ZoneOffset?,
    ) = upsert(night, start, end, zoneOffset, SleepRecordingMethod.MANUAL)

    /** 그 밤에 우리가 쓴 세션을 지운다. */
    suspend fun deleteRecordedSleep(night: LocalDate) = gateway.deleteByClientRecordId(clientRecordId(night))

    /** `[start, end)` 구간에 우리가 아닌 다른 앱이 쓴 수면 세션이 있으면 true. */
    suspend fun hasExternalSleep(
        start: Instant,
        end: Instant,
    ): Boolean = gateway.read(start, end).any { it.originPackageName != selfPackageName }

    /** `[start, end)` 구간에 (우리·외부 무관) 수면 세션이 하나라도 있으면 true. 사용자 입력 유도 판단용. */
    suspend fun hasAnySleep(
        start: Instant,
        end: Instant,
    ): Boolean = gateway.read(start, end).isNotEmpty()

    private suspend fun upsert(
        night: LocalDate,
        start: Instant,
        end: Instant,
        zoneOffset: ZoneOffset?,
        method: SleepRecordingMethod,
    ) = gateway.upsert(
        SleepWrite(
            clientRecordId = clientRecordId(night),
            start = start,
            end = end,
            zoneOffset = zoneOffset,
            version = end.toEpochMilli(),
            recordingMethod = method,
        ),
    )

    private fun clientRecordId(night: LocalDate): String = "$CLIENT_RECORD_ID_PREFIX$night"

    companion object {
        /** 우리 앱이 쓴 수면 세션 식별 접두사. 밤 날짜(ISO)와 합쳐 clientRecordId 를 만든다. */
        const val CLIENT_RECORD_ID_PREFIX = "laimory-sleep-"
    }
}
