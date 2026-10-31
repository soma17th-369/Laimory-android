package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/**
 * Event 장소 지정 요청. 후보에 없는 이름도 받는다(직접 입력).
 *
 * 서버는 누락·공백뿐·255자 초과를 400 으로 거절한다. 좌표는 쌍이고, 둘 다 있으면 이벤트 좌표를 함께 바꾸며
 * null 이면 지금 좌표를 그대로 둔다. 장소 이름 · 좌표는 위치 정보라 로그에 남기지 않는다.
 */
@Serializable
data class UpdateTimelineEventPlaceRequest(
    val place: String,
    // 기본값을 두지 않는다. 기본값이 있으면 직렬화에서 키가 빠지는데, 좌표 없음은 null 로 드러내 보낸다.
    val latitude: Double?,
    val longitude: Double?,
)
