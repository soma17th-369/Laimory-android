package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDate

/**
 * draft 생성이 완료된 하루 타임라인 기록.
 *
 * [status]는 화면 권한(저장·편집·삭제)과 저장 CTA 노출의 정본이다.
 * null은 상태를 알 수 없는 응답(구계약·미지원 값)으로, 읽기 전용으로 다룬다.
 */
data class DailyTimeline(
    val dailyRecordId: Long,
    val recordDate: LocalDate,
    val emotion: TimelineEmotion?,
    val events: List<TimelineEvent>,
    val status: DailyRecordStatus? = null,
)
