package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/**
 * Event 장소 지정 요청. 후보에 없는 이름도 받는다(직접 입력).
 *
 * 서버는 누락·공백뿐·255자 초과를 400 으로 거절한다. 장소 이름은 위치 정보라 로그에 남기지 않는다.
 */
@Serializable
data class UpdateTimelineEventPlaceRequest(
    val place: String,
)
