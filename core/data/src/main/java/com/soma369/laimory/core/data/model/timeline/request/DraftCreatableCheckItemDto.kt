package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/** 생성 가능 확인 요청의 아이템 1건. 시각은 초안 요청([SourceItemDto])과 같은 로컬 datetime 이다. */
@Serializable
data class DraftCreatableCheckItemDto(
    val itemType: String,
    val startAt: String,
    val endAt: String? = null,
)
