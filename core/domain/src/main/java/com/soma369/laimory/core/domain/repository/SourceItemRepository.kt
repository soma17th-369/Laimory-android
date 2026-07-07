package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 수집 아이템 로컬 저장 계약.
 *
 * 구현은 `:core:collection` 이 소유한다 — 수집 영역의 저장(Room)은
 * `:core:data` 의 remote 책임과 분리해 수집 모듈이 자체 소유한다.
 */
interface SourceItemRepository {
    /**
     * 아이템을 저장한다. `sourceKey` 가 이미 존재하는 아이템은 무시한다(멱등).
     *
     * @return 실제로 새로 저장된 아이템 수
     */
    suspend fun addAll(items: List<SourceItem>): Int

    /** 저장된 전체 아이템을 이벤트 시각 내림차순으로 관찰한다. */
    fun observeAll(): Flow<List<SourceItem>>

    /** 해당 카테고리에서 마지막으로 수집된 시각. 증분 수집 커서로 사용한다. 없으면 null. */
    suspend fun getLatestCollectedAt(itemType: ItemType): Instant?

    /** 해당 카테고리에 저장된 아이템을 모두 삭제한다(스테이징 일괄 비우기). */
    suspend fun clear(itemType: ItemType)
}
