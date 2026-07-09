package com.soma369.laimory.core.collection.health

/**
 * 수면 신뢰도 표본의 영속 버퍼 seam.
 *
 * `SleepClassifyEvent` 는 하루 내내 ~10분 간격으로, 앱이 죽어있는 동안에도 도착하므로 인메모리로는 못 모은다 →
 * 구현([DataStoreSleepClassifyStore])이 최근 표본을 영속한다. 테스트는 인메모리 페이크로 대체한다.
 */
internal interface SleepClassifyStore {
    /** 표본을 추가한다(오래된 것부터 잘려 최근 것만 유지). */
    suspend fun add(samples: List<SleepClassifySample>)

    /** `[startMillis, endMillis]` 창에 속한 표본의 신뢰도 목록. */
    suspend fun confidencesInWindow(
        startMillis: Long,
        endMillis: Long,
    ): List<Int>
}
