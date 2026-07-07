package com.soma369.laimory.core.collection.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 수집 아이템 로컬 저장 테이블.
 *
 * - 이벤트 시각의 source of truth 는 UTC epoch millis 로 저장하고,
 *   업로드 포맷의 local datetime 문자열은 [timeZoneId] 로 렌더링해서 만든다.
 * - (itemType, sourceName, sourceKey) 복합 UNIQUE 인덱스가 재스캔 중복 저장을 막는다(멱등).
 *   [sourceKey] 는 같은 원천(sourceName) 안에서만 유일하면 된다.
 * - [payloadJson] 은 카테고리별 payload 를 JSON 으로 직렬화한 값이다.
 *   카테고리별 필드 추가는 스키마 변경 없이 payload 안에서 이루어진다.
 */
@Entity(
    tableName = "source_item",
    indices = [
        Index(value = ["itemType", "sourceName", "sourceKey"], unique = true),
        Index(value = ["itemType", "collectedAtUtc"]),
    ],
)
internal data class SourceItemEntity(
    @PrimaryKey val rawId: String,
    val itemType: String,
    val startAtUtc: Long,
    val endAtUtc: Long?,
    val timeZoneId: String,
    val payloadJson: String,
    val sourceName: String,
    val sourceKey: String,
    val collectedAtUtc: Long,
)
