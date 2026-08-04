package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/** Event 메모 작성·수정·제거 요청. null은 서버의 메모 제거를 의미한다. */
@Serializable
data class UpdateTimelineEventMemoRequest(
    val memo: String?,
)
