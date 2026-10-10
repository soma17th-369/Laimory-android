package com.soma369.laimory.core.data.model.onboarding

import kotlinx.serialization.Serializable

/**
 * `GET /initializer` 응답. 앱 시작에 필요한 계정 단위 설정이다.
 *
 * @property popupNotices 앱 시작 팝업으로 띄울 공지(최신 순) — 제목·썸네일까지 실려 온다. **기본값이 꼭 있어야
 * 한다** — 운영 서버가 이 필드를 싣기 전에는 키가 없다. 원문 주소는 없어 원문을 열 때 공지 단건 조회로 받는다.
 */
@Serializable
data class AppInitializerResponse(
    val onboardingCompleted: Boolean,
    val popupNotices: List<PopupNoticeResponse> = emptyList(),
)
