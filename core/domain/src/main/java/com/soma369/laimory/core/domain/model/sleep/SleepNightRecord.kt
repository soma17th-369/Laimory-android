package com.soma369.laimory.core.domain.model.sleep

import java.time.Instant

/**
 * 그 밤 창에 이미 존재하는 수면 기록(Health Connect) 한 건.
 *
 * [isOurs] 는 우리 앱이 쓴 기록인지(=우리 밤별 clientRecordId) 여부다. 화면 표시용이 아니라 저장 정책
 * 판단에 쓴다 — 외부(삼성헬스 등) 기록은 HC 상 우리가 덮어쓸 수 없어 저장을 막고, 우리 기록이면 덮어쓴다(upsert).
 */
data class SleepNightRecord(
    val start: Instant,
    val end: Instant,
    val isOurs: Boolean,
)
