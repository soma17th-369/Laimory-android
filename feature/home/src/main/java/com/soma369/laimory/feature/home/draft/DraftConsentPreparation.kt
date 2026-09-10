package com.soma369.laimory.feature.home.draft

import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import java.time.LocalDate
import java.time.ZoneId

/**
 * CTA 를 눌러 확정한 **제출용** 스냅샷.
 *
 * 화면에 보인 데이터와 서버로 전송되는 데이터의 일치를 이 객체가 보장한다 — 확정한 뒤에는
 * 수집이 갱신돼도 바뀌지 않는다. 상시 열람용 스냅샷([DraftConsentSelectionSnapshot])과 달리
 * 제출·폐기로만 사라진다.
 *
 * @param attemptId 제출 시도 식별자. CTA 를 누를 때마다 증가한다.
 * @param discardActiveTask 실패한 이전 초안 작업을 제출 직전에 폐기해야 하는지 여부.
 */
data class DraftConsentPreparation(
    val attemptId: Long,
    val snapshot: DraftConsentSelectionSnapshot,
    val discardActiveTask: Boolean,
) {
    val recordDate: LocalDate get() = snapshot.recordDate
    val zone: ZoneId get() = snapshot.zone
    val window: RecordDateWindow get() = snapshot.window
    val selection: DraftSourceItemSelection get() = snapshot.selection
}
