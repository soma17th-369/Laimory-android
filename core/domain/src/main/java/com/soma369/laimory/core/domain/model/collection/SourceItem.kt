package com.soma369.laimory.core.domain.model.collection

import java.time.Instant
import java.time.ZoneId

/**
 * 수집된 라이프로그 원천 이벤트 1건.
 *
 * 로컬 저장의 기본 단위이며, 서버 업로드 envelope
 * (`recordDate`/`recordTimeZone`/`timelineWindow`/`sourceItems[]`)는 업로드 시점에 이 아이템들을
 * 묶어 조립한다 — 로컬에는 envelope 를 저장하지 않는다.
 *
 * - [rawId]: 아이템 식별자. 수집기가 생성하며 저장 후 변하지 않는다.
 * - [startAt]/[endAt]: 이벤트 시각의 source of truth 는 UTC instant 다.
 *   업로드 포맷의 local datetime 문자열은 [timeZoneId] 로 렌더링해서 만든다.
 * - [endAt]: 단일 시점 이벤트(사진, 알림)는 null 을 허용한다.
 * - [sourceName]/[sourceKey]: 재스캔 중복 방지용 자연키. 같은 원천 이벤트는 항상 같은 값이
 *   나오도록 수집기가 원천 식별자(MediaStore ID, Calendar instance ID 등)에서 파생한다.
 *   sourceKey 는 같은 [sourceName] 안에서만 유일하면 되며, 중복 제거의 unique 기준은
 *   (itemType, sourceName, sourceKey) 조합이다.
 * - [collectedAt]: 수집 시각. 증분 수집 커서의 기준이 된다.
 */
data class SourceItem(
    val rawId: String,
    val startAt: Instant,
    val endAt: Instant?,
    val timeZoneId: ZoneId,
    val payload: SourceItemPayload,
    val sourceName: SourceName,
    val sourceKey: String,
    val collectedAt: Instant,
) {
    val itemType: ItemType get() = payload.itemType
}
