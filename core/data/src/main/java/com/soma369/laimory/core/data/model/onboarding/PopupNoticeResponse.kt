package com.soma369.laimory.core.data.model.onboarding

import com.soma369.laimory.core.domain.model.notice.PopupNotice
import kotlinx.serialization.Serializable

/** 앱 초기화 응답의 팝업 공지 한 건. 세 필드 모두 서버가 늘 싣는다(팝업 지정에 썸네일 필수). 원문 주소는 없다. */
@Serializable
data class PopupNoticeResponse(
    val noticeId: Long,
    val title: String,
    val thumbnailUrl: String,
)

internal fun PopupNoticeResponse.toDomain(): PopupNotice = PopupNotice(id = noticeId, title = title, thumbnailUrl = thumbnailUrl)
