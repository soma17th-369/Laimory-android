package com.soma369.laimory.core.domain.model.settings

import java.time.LocalTime

/**
 * 홈이 처음 그리는 기록 범위. 사용자가 `기본값으로 지정` 으로 기기에 남긴 값이다.
 *
 * 날짜에 붙이기 전의 **시각 모양**만 담는다 — 시작 시각과 종료가 당일인지 익일인지, 종료 시각. 그래서 어느
 * 날짜를 고르든 그 날짜에 그대로 얹힌다. 날짜에 얹은 실제 구간은 `RecordDateWindow` 다.
 *
 * 범위가 지켜야 하는 제약(최소 길이·종료 상한)은 홈이 판정한다. 저장된 값이 그 제약에 맞는지는 읽는 쪽이
 * 다시 본다 — 정책이 바뀌면 예전에 저장한 값이 맞지 않을 수 있다.
 */
data class DefaultRecordRange(
    val startTime: LocalTime,
    val endsNextDay: Boolean,
    val endTime: LocalTime,
) {
    companion object {
        /**
         * 저장한 값이 없을 때. 하루의 시작을 06:00 으로 보는 홈 기본 날짜 규칙과 경계를 맞췄다 — 새벽에 연
         * 홈은 어제를 고르는데, 그 어제가 지금 이 순간까지 담는다.
         */
        val INITIAL = DefaultRecordRange(startTime = LocalTime.of(6, 0), endsNextDay = true, endTime = LocalTime.of(6, 0))
    }
}
