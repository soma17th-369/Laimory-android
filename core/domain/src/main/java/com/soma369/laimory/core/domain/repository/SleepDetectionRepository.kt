package com.soma369.laimory.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 수면 자동 감지(Sleep API → HC 기록) 온보딩 상태 계약(에픽 #142). 구현은 `:core:collection` 이 소유한다.
 *
 * "감지 원함" 의도를 영속해 콜드스타트/부팅에도 구독을 복원한다. 켜면 즉시 구독을 걸어(콜드 재실행 불필요)
 * 그날 밤부터 감지가 동작한다. 실제 권한(활동 인식·HC 쓰기) 확보는 화면이 선행한다.
 */
interface SleepDetectionRepository {
    /** 자동 감지 활성(사용자 의도) 여부를 관찰한다. */
    fun observeEnabled(): Flow<Boolean>

    /** 자동 감지를 켜거나 끈다. 켜면 의도 영속 + 즉시 구독, 끄면 구독 해제한다. */
    suspend fun setEnabled(enabled: Boolean)
}
