package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import java.time.LocalDate

/**
 * 서버 감정 literal 을 도메인 감정으로 옮긴다. 모르는 값은 [TimelineEmotion.UNKNOWN] 으로 수렴한다.
 *
 * 감정 파싱과 날짜 파싱은 일일·월별 응답이 똑같이 하는 일이라 한곳에 둔다 — 각자 복사하면
 * "모르는 literal 을 어떻게 다룰지" 정책이 응답마다 갈린다.
 */
internal fun String?.toTimelineEmotionOrNull(): TimelineEmotion? =
    this?.let { raw -> TimelineEmotion.entries.firstOrNull { it.name == raw } ?: TimelineEmotion.UNKNOWN }

internal fun String.parseLocalDate(fieldName: String): LocalDate =
    runCatching { LocalDate.parse(this) }
        .getOrElse { throw ApiException.UnknownException("잘못된 $fieldName 형식입니다: $this") }
