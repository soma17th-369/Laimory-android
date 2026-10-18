package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/**
 * `POST /timeline/drafts/check` 요청. 이 목록으로 타임라인을 만들 수 있는지 서버에 묻는다.
 *
 * 판정에 쓰는 시각만 보낸다 — payload(알림 본문·좌표 등)와 rawId 는 싣지 않는다. 같은 개인정보를 판정 때문에
 * 한 번 더 내보내지 않는다.
 */
@Serializable
data class DraftCreatableCheckRequest(
    val sourceItems: List<DraftCreatableCheckItemDto>,
)
