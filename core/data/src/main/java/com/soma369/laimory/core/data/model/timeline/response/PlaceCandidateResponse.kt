package com.soma369.laimory.core.data.model.timeline.response

import kotlinx.serialization.Serializable

/** Event 장소 후보 하나. 서버는 이름만 준다(거리·업종·좌표 없음). */
@Serializable
data class PlaceCandidateResponse(
    val name: String,
)
