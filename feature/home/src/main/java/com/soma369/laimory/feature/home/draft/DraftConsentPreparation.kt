package com.soma369.laimory.feature.home.draft

import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import java.time.LocalDate
import java.time.ZoneId

/**
 * 현재 생성 시도 1회의 불변 스냅샷.
 *
 * 동의 화면의 요약·유형별 상세·실제 제출이 모두 이 [selection] 하나를 참조한다 —
 * 화면에 보인 데이터와 서버로 전송되는 데이터의 일치를 이 객체가 보장한다.
 *
 * @param attemptId 생성 시도 식별자. 홈에서 동의 화면으로 진입할 때마다 증가하며,
 *   동일 데이터로 재진입해도 새 시도로 구분해 체크 상태를 초기화하는 기준이 된다.
 * @param discardActiveTask 실패한 이전 초안 작업을 제출 직전에 폐기해야 하는지 여부.
 */
data class DraftConsentPreparation(
    val attemptId: Long,
    val recordDate: LocalDate,
    val zone: ZoneId,
    val window: RecordDateWindow,
    val selection: DraftSourceItemSelection,
    val discardActiveTask: Boolean,
)
