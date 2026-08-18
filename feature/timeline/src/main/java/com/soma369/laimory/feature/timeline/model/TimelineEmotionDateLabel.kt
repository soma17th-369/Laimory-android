package com.soma369.laimory.feature.timeline.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 감정 시트 안내 문구가 가리키는 날짜 표현.
 *
 * 초안은 자정을 넘겨 저장될 수 있어 "오늘"만으로는 어느 하루인지 어긋난다. 오늘·어제는 말로 짚고
 * 그보다 지난 날짜는 `MM.DD`로 명시해 사용자가 어떤 하루의 감정을 고르는지 헷갈리지 않게 한다.
 */
fun timelineEmotionDateLabel(
    recordDate: LocalDate,
    today: LocalDate,
): String =
    when (recordDate) {
        today -> "오늘"
        today.minusDays(1) -> "어제"
        else -> recordDate.format(EmotionDateFormatter)
    }

private val EmotionDateFormatter = DateTimeFormatter.ofPattern("MM.dd")
