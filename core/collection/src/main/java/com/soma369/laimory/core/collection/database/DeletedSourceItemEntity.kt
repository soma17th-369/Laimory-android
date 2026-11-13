package com.soma369.laimory.core.collection.database

import androidx.room.Entity

/**
 * 사용자가 기기에서 지운 수집 아이템의 묘비.
 *
 * 지운 행이 수집 경로에서 같은 자연키로 되살아나지 않게 막는다. 진행 중인 체류는 같은
 * `STAY:<시작 시각>` 키로 계속 갱신되고, 알림은 게시 뒤 클릭 이벤트가 같은 키로 다시 들어온다.
 *
 * **내용(payload)은 남기지 않는다.** 지운 것을 다시 볼 수 없어야 지운 의미가 있다. 남기는 것은
 * 자연키와 만료 판정에 쓰는 이벤트 시각뿐이고, 원래 행과 같은 보존 기간 규칙으로 함께 지워진다.
 */
@Entity(
    tableName = "deleted_source_item",
    primaryKeys = ["itemType", "sourceName", "sourceKey"],
)
internal data class DeletedSourceItemEntity(
    val itemType: String,
    val sourceName: String,
    val sourceKey: String,
    val startAtUtc: Long,
    val endAtUtc: Long?,
    val deletedAtUtc: Long,
)
