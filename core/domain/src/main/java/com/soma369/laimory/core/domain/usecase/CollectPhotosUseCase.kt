package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.collector.Collector
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.repository.SourceItemRepository

/**
 * 사진 수집기를 실행하고 결과를 로컬 저장소에 저장한다.
 *
 * PHOTO collector 를 [collectors] 레지스트리에서 찾아 실행한 뒤 [SourceItemRepository.addAll] 로 저장하고,
 * 실제로 새로 저장된 건수를 반환한다(중복은 저장 계층 멱등이 무시). PHOTO collector 가 등록돼 있지 않으면 0.
 *
 * [collectors] 는 후속 #92~#95 수집기 확장을 위한 [ItemType] 키 레지스트리다(Hilt multibinding).
 */
class CollectPhotosUseCase(
    private val collectors: Map<ItemType, Collector>,
    private val repository: SourceItemRepository,
) {
    suspend operator fun invoke(): Int {
        val collector = collectors[ItemType.PHOTO] ?: return 0
        return repository.addAll(collector.collect())
    }
}
