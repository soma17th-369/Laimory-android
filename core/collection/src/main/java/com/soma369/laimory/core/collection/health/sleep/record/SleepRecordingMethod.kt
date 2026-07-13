package com.soma369.laimory.core.collection.health.sleep.record

/**
 * 수면 세션을 Health Connect 에 어떤 방식으로 기록했는지.
 *
 * Sleep API 자동 감지분은 [AUTO_DETECTED], 사용자 수동 입력분은 [MANUAL] 로 표시해
 * HC 의 recordingMethod 에 반영한다 — 다른 앱/사용자가 출처를 구분할 수 있게 한다.
 */
internal enum class SleepRecordingMethod {
    AUTO_DETECTED,
    MANUAL,
}
