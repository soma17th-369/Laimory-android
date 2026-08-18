package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/**
 * 하루 기록 작성 완료(`DRAFT → SAVED`) 요청.
 *
 * 서버는 하루 감정을 필수 non-null 로 받고 누락·null·미지원 literal 을 `400/-400` 으로 거절한다.
 */
@Serializable
data class SaveDailyRecordRequest(
    val emotionType: String,
)
