package com.soma369.laimory.core.domain.model.timeline

/**
 * 위치 상세 지도를 실제로 렌더링해도 되는지.
 *
 * 지도를 그리는 것 자체가 카메라 영역을 Google 로 보내는 일이다. 그 영역은 사용자의 체류
 * 좌표에서 계산되므로, 동의 없이 지도를 붙이면 "무엇이 기기 밖으로 나가는지 확인받는 화면"이
 * 확인 전에 위치를 내보내게 된다. 그래서 `GoogleMap` 을 composition 에 **넣기 전에** 이 게이트를 본다.
 *
 * 판정의 정본은 저장된 위치정보 약관 동의다([com.soma369.laimory.core.domain.model.terms.TermStage.TIMELINE_LOCATION]).
 * 동의 이력은 서버가 갖고 있어 조회가 필요하므로 이 판정은 suspend 다 — 알아내기 전까지는
 * "허용되지 않음"이고, 조회에 실패해도 마찬가지다. 모르는 상태에서 좌표를 내보내지 않는다.
 *
 * API 키가 없어도 빌드는 성공해야 하므로 키 준비 상태도 이 게이트가 함께 본다. 조립은 앱 계층이
 * 한다 — 빌드 설정을 읽는 자리라
 * [com.soma369.laimory.core.domain.model.collection.CollectionLabAccessGate] 와 같은 포트다.
 */
fun interface LocationMapRenderGate {
    suspend fun isMapRenderAllowed(): Boolean
}
