package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.collection.SourceItem

/**
 * 사용자가 수집 아이템을 기기에서 바로 지우는 계약.
 *
 * 보존 기간 만료([SourceItemRepository.deleteExpired])와 달리 **지운 자연키가 수집으로 되살아나지 않는다.**
 * 진행 중인 체류처럼 같은 키로 계속 갱신되는 항목도 지운 뒤에는 다시 저장되지 않는다.
 */
interface SourceItemDeletionRepository {
    /**
     * 아이템을 지운다. 없는 rawId 는 건너뛴다.
     *
     * @return 실제로 지운 아이템. 실행 취소 때 [restore] 에 그대로 넘긴다.
     */
    suspend fun delete(rawIds: Collection<String>): List<SourceItem>

    /** 지웠던 아이템을 되돌리고, 그 자연키를 다시 수집할 수 있게 한다. */
    suspend fun restore(items: List<SourceItem>)
}
