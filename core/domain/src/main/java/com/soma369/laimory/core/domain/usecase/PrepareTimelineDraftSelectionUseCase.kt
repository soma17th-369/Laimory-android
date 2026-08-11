package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReporter
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 초안 생성에 실제로 전송될 아이템 목록을 네트워크 부작용 없이 확정한다.
 *
 * 기록 창 필터·타입별 상한·정렬을 적용한 [DraftSourceItemSelection]을 반환하며,
 * 동의 화면이 이 결과를 불변 스냅샷으로 삼아 표시·상세·제출에 동일하게 사용한다.
 * 사용자 선택 PHOTO가 상한을 초과하면 자동 절삭하지 않고 실패한다.
 * 선택 측정 리포트는 확정 시점에 한 번만 발행한다(제출 단계에서 재발행하지 않는다).
 */
@Singleton
class PrepareTimelineDraftSelectionUseCase
    @Inject
    constructor(
        private val selectionPolicy: DraftSourceItemSelectionPolicy,
        private val selectionReporter: DraftSourceItemSelectionReporter,
    ) {
        operator fun invoke(
            window: RecordDateWindow,
            items: List<SourceItem>,
        ): Result<DraftSourceItemSelection> =
            runCatching {
                selectionPolicy.select(window, items).getOrThrow().also { selection ->
                    if (selectionReporter.isEnabled) selectionReporter.reportSelection(selection.report)
                }
            }
    }
