package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 앱 진입 시 기본으로 선택할 기록 날짜 규칙 — "다음날 아침(정오 전)에 어제 일기를 쓴다" UX.
 *
 * 현재 시각이 정오(12:00) 이전이면 어제, 정오부터는 오늘이 기본값이다. 기본값일 뿐이므로
 * 사용자가 date picker 로 고른 날짜가 항상 우선하며, 서버 전송은 선택된 날짜 그대로다 —
 * 서버는 이 규칙을 모른다(과거 서버 정오 경계 파생은 삭제됨).
 */
object DefaultRecordDate {
    fun at(now: LocalDateTime): LocalDate = if (now.toLocalTime() < LocalTime.NOON) now.toLocalDate().minusDays(1) else now.toLocalDate()
}
