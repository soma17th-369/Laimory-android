package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/** 서버가 초안 생성에 사용할 오프셋 없는 로컬 datetime 반열린 구간 `[startTime, endTime)`. */
@Serializable
data class TimelineWindowDto(
    val startTime: String,
    val endTime: String,
)
