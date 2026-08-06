package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.SourceItem

/** 위치 분절기의 진행 상태와 확정 SourceItem을 원자적으로 저장하는 수집기 내부 영속 seam. */
internal interface LocationSegmentStore {
    suspend fun restore(): LocationSegmentSnapshot?

    suspend fun persist(
        snapshot: LocationSegmentSnapshot?,
        items: List<SourceItem>,
    )

    suspend fun awaitIdle()
}
