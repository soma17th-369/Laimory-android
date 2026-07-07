package com.soma369.laimory.core.domain.model.collection

/**
 * 수집 아이템의 원천 시스템.
 *
 * `sourceKey` 는 같은 원천 안에서만 유일하면 된다 — 재스캔 중복 제거의 unique 기준은
 * (itemType, sourceName, sourceKey) 조합이다.
 */
enum class SourceName {
    MEDIA_STORE,
    CALENDAR_PROVIDER,
    HEALTH_CONNECT,
    LOCATION_PROVIDER,
    NOTIFICATION_LISTENER,
}
