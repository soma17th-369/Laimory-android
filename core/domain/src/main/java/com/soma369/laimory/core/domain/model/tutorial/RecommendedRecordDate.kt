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
 * @property photoUris 미리 보여 줄 직접 찍은 사진의 content URI. 찍은 차례(이른 것부터)로 최대 [PREVIEW_PHOTO_COUNT] 장이고,
 *   더 많이 찍은 날은 하루 전체에서 고르게 뽑는다. 비어 있지 않다(찍은 사진이 없는 날은 추천하지 않는다).
 */
data class RecommendedRecordDate(
    val date: LocalDate,
    val capturedPhotoCount: Int,
    val calendarEventCount: Int,
    val photoUris: List<String>,
) {
    companion object {
        /** 카드가 미리 보여 주는 사진 수 — 3열 그리드 여덟 줄. 많이 찍은 날도 한 바퀴가 너무 길지 않게 끊는다. */
        const val PREVIEW_PHOTO_COUNT = 24
    }
}
