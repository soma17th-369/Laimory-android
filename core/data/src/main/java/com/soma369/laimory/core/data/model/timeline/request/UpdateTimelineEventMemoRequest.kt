package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/**
 * Event 메모 작성·수정·제거 요청.
 *
 * 서버는 `memo` 가 null·공백이거나 **키가 없어도** 제거로 본다. 기본값을 줘 제거를 `{}` 로 보낸다 —
 * 셋 다 같은 뜻이라 지금 나가던 모양을 그대로 둔다.
 */
@Serializable
data class UpdateTimelineEventMemoRequest(
    val memo: String? = null,
)
