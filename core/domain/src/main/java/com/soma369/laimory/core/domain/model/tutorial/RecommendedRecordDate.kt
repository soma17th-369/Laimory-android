package com.soma369.laimory.core.domain.model.tutorial

import java.time.LocalDate

/**
 * 튜토리얼이 첫 타임라인을 만들어 보라고 추천하는 날 하나.
 *
 * 건수는 그 날의 기록 범위(기본 06:00~익일 06:00)로 센다.
 *
 * @property capturedPhotoCount 직접 찍은 사진 수. 캡처처럼 촬영 정보가 없는 이미지는 빠지므로 그 날 홈 사진 카드의
 *   수(모든 이미지)보다 적을 수 있다.
 * @property calendarEventCount 개인 캘린더 일정 수. 홈 일정 카드와 같은 규칙으로 센다.
 * @property coverPhotoUri 대표로 보여 줄 직접 찍은 사진 한 장의 content URI.
 */
data class RecommendedRecordDate(
    val date: LocalDate,
    val capturedPhotoCount: Int,
    val calendarEventCount: Int,
    val coverPhotoUri: String,
)
