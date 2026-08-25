package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기록 창과 겹치는 현재 저장 항목을 한 번 읽는다.
 *
 * 자동 수집을 마친 직후 전송 스냅샷을 확정할 때 쓴다. 화면이 들고 있던 관찰 결과는 수집이
 * 반영되기 전 값일 수 있어, 확정 시점에는 저장소를 직접 다시 본다.
 */
@Singleton
class GetSourceItemsInWindowUseCase
    @Inject
    constructor(
        private val repository: SourceItemRepository,
    ) {
        suspend operator fun invoke(window: RecordDateWindow): List<SourceItem> = repository.getInWindow(window.start, window.end)
    }
