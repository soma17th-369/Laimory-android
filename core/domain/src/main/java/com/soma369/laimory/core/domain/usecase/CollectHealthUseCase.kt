package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.collector.Collector
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.repository.SourceItemRepository

/**
 * 걸음수·수면 등 건강 데이터를 수집해 로컬 저장소에 저장한다.
 *
 * HEALTH collector 를 [collectors] 레지스트리에서 찾아 실행한 뒤 [SourceItemRepository.upsertAll] 로 저장한다.
 * 걸음수 일별 집계는 하루 동안 값이 변하는 aggregate 라, 불변 이벤트용 addAll(멱등 무시) 대신 upsert 로
 * 같은 날짜 bucket 을 갱신한다(최초 rawId 유지). 반환값은 새로 삽입된 건수. HEALTH collector 가 없으면 0.
 */
class CollectHealthUseCase(
    private val collectors: Map<ItemType, Collector>,
    private val repository: SourceItemRepository,
) {
    suspend operator fun invoke(): Int {
        val collector = collectors[ItemType.HEALTH] ?: return 0
        return repository.upsertAll(collector.collect())
    }
}
