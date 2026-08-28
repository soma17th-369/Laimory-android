package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/**
 * 저장된 하루 기록의 감정 교체 요청.
 *
 * 서버는 `emotionType` 을 필수 non-null 로 받고 누락·null·미지원 literal 을 `400/-400` 으로 거절한다.
 * 같은 값 재요청은 멱등 성공이다.
 */
@Serializable
data class UpdateDailyRecordEmotionRequest(
    val emotionType: String,
)
