package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.collector.Collector
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.repository.SourceItemRepository

/**
 * 개인 캘린더 일정을 수집해 로컬 저장소에 저장한다.
 *
 * CALENDAR collector 를 [collectors] 레지스트리에서 찾아 실행한 뒤 [SourceItemRepository.addAll] 로 저장하고,
 * 실제로 새로 저장된 건수를 반환한다(중복은 저장 계층 멱등이 무시). CALENDAR collector 가 없으면 0.
 */
class CollectCalendarUseCase(
    private val collectors: Map<ItemType, Collector>,
    private val repository: SourceItemRepository,
) {
    suspend operator fun invoke(): Int {
        val collector = collectors[ItemType.CALENDAR] ?: return 0
        return repository.addAll(collector.collect())
    }
}
