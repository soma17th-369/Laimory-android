package com.soma369.laimory.core.collection.health

/**
 * Sleep API 수면 구간의 감지 품질(GMS `SleepSegmentEvent.status` 매핑).
 *
 * - [DETECTED]: 데이터가 충분해 신뢰할 만하게 감지됨(`STATUS_SUCCESSFUL`) — 신뢰도 게이트 후보.
 * - [MISSING_DATA]: 감지했으나 데이터에 공백이 있음(`STATUS_MISSING_DATA`) — 자동 기록하지 않고 불확실로 본다.
 * - [NOT_DETECTED]: 수면을 감지하지 못함(`STATUS_NOT_DETECTED`).
 */
internal enum class SleepDetectionStatus {
    DETECTED,
    MISSING_DATA,
    NOT_DETECTED,
}
