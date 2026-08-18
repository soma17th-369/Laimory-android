package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDate

/**
 * 초안 작업이 처음 완료됐다는 일회성 신호.
 *
 * [recordDate]는 서버 응답의 결과 타임라인에서 온다. FCM payload에는 날짜가 없고, 활성 작업이
 * 없는 경로도 있어 결과가 유일하게 믿을 수 있는 이동 대상이다.
 */
data class DraftTaskCompletion(
    val taskId: String,
    val recordDate: LocalDate,
)
