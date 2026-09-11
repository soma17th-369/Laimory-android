package com.soma369.laimory.feature.home.draft

import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import java.time.LocalDate
import java.time.ZoneId

/**
 * 홈이 상시로 유지하는 선택 스냅샷.
 *
 * CTA 를 누르기 전에도 카드에서 상세를 열어 고르므로, 스냅샷은 **생성 시도보다 먼저** 있어야 한다.
 *
 * [revision] 은 기록 창이 바뀌거나 수집이 갱신될 때마다 오른다. **표시 모델만 다시 파생하는
 * 신호이지 선택 상태를 비우는 경계가 아니다** — 수집이 돌 때마다 사용자가 고른 것이 사라지면
 * 카드에서 무엇을 뺄 수가 없다. 제외 집합은 사라진 항목만 걷어 내고, 비우는 것은 제출 성공뿐이다.
 */
data class DraftConsentSelectionSnapshot(
    val revision: Long,
    val recordDate: LocalDate,
    val zone: ZoneId,
    val window: RecordDateWindow,
    val selection: DraftSourceItemSelection,
)
