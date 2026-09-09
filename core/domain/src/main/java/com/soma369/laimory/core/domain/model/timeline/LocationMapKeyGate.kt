package com.soma369.laimory.core.domain.model.timeline

/**
 * 지도 SDK 키가 심겨 있는지. **키 준비 여부만** 답한다.
 *
 * 키가 비면 SDK 인증이 실패하므로 `GoogleMap` 을 붙이지 않고 대체 안내로 넘긴다. 키 없는 개발
 * 환경이나 CI 에서도 빌드는 성공해야 하므로 값이 없을 수 있다.
 *
 * **지도를 그려도 되는지는 여기서 답하지 않는다.** 그 판단은 저장된 위치정보 약관 동의이고
 * ([com.soma369.laimory.core.domain.model.terms.TermStage.TIMELINE_LOCATION]), 같은 동의가 지도뿐
 * 아니라 좌표를 기기 밖으로 내보내는 다른 일 — 표시용 주소 해석 — 도 함께 가른다. 두 조건을 한
 * 포트에 합치면 지도만 막고 주소 해석은 열어 두는 어긋남이 생긴다.
 *
 * 빌드 설정을 읽는 자리라
 * [com.soma369.laimory.core.domain.model.collection.CollectionLabAccessGate] 와 같은 포트다.
 */
fun interface LocationMapKeyGate {
    fun isMapKeyPresent(): Boolean
}
