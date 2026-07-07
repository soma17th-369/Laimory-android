package com.soma369.laimory.core.domain.model.collection

/**
 * 수집 아이템의 카테고리 타입.
 *
 * 서버 업로드 포맷의 `sourceItems[].itemType` 값과 1:1 매핑되며,
 * 로컬 저장 시에도 같은 이름의 문자열로 저장한다.
 */
enum class ItemType {
    LOCATION,
    MOVEMENT,
    CALENDAR,
    HEALTH,
    NOTIFICATION,
    PHOTO,
}
