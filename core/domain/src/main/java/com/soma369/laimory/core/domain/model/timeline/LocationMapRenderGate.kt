package com.soma369.laimory.core.domain.model.timeline

/**
 * 위치 상세 지도를 실제로 렌더링해도 되는지.
 *
 * 지도를 그리는 것 자체가 카메라 영역을 Google 로 보내는 일이다. 그 영역은 사용자의 체류
 * 좌표에서 계산되므로, 동의 없이 지도를 붙이면 "무엇이 기기 밖으로 나가는지 확인받는 화면"이
 * 확인 전에 위치를 내보내게 된다. 그래서 `GoogleMap` 을 composition 에 **넣기 전에** 이 게이트를 본다.
 *
 * 정본은 계정 단위 최초 1회 동의(#238)다. 그 동의 저장소가 생기기 전까지는
 * [com.soma369.laimory.core.domain.model.timeline.DraftConsentSubmissionGate] 와 같은 방식으로
 * 빌드 단위 임시 바인딩을 쓴다 — 릴리즈에서는 켜지지 않는다.
 *
 * API 키가 없어도 빌드는 성공해야 하므로 키 준비 상태도 이 게이트가 함께 본다.
 */
fun interface LocationMapRenderGate {
    fun isMapRenderAllowed(): Boolean
}
