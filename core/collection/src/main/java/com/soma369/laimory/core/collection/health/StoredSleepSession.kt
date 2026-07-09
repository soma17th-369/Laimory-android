package com.soma369.laimory.core.collection.health

import java.time.Instant

/**
 * Health Connect 에서 읽은 수면 세션(정합성 판정에 필요한 최소 필드).
 *
 * [originPackageName] 으로 우리 앱이 쓴 것과 외부 앱(삼성헬스 등) 기록을 구분한다 —
 * "그 밤에 외부 수면 기록이 있으면 우리는 쓰지 않는다" 규칙의 판단 재료.
 */
internal data class StoredSleepSession(
    val start: Instant,
    val end: Instant,
    val clientRecordId: String?,
    val originPackageName: String,
)
