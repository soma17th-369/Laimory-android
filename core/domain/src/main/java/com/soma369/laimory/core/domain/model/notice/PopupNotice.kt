package com.soma369.laimory.core.domain.model.notice

/**
 * 앱 시작 팝업 공지 한 건. 앱 초기화 응답에 실려 온다 — 팝업을 그리는 데 필요한 것(제목·썸네일)만 있다.
 *
 * 원문 주소는 없다. 원문을 열 때 공지 단건 조회로 받는다(그사이 숨겨졌으면 열지 않는다).
 *
 * @param thumbnailUrl CDN 이미지 주소. 서버가 팝업 지정에 썸네일을 필수로 하므로 늘 있다.
 */
data class PopupNotice(
    val id: Long,
    val title: String,
    val thumbnailUrl: String,
)
