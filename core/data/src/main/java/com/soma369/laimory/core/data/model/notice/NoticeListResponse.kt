package com.soma369.laimory.core.data.model.notice

import kotlinx.serialization.Serializable

/** 노출 중인 공지 목록. 공지가 없으면 404 가 아니라 빈 배열이다. */
@Serializable
data class NoticeListResponse(
    val notices: List<NoticeResponse> = emptyList(),
)
