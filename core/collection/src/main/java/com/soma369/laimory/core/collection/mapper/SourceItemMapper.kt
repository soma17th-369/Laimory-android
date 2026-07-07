package com.soma369.laimory.core.collection.mapper

import com.soma369.laimory.core.collection.database.SourceItemEntity
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import java.time.Instant
import java.time.ZoneId

internal fun SourceItem.toEntity(): SourceItemEntity =
    SourceItemEntity(
        rawId = rawId,
        itemType = itemType.name,
        startAtUtc = startAt.toEpochMilli(),
        endAtUtc = endAt?.toEpochMilli(),
        timeZoneId = timeZoneId.id,
        payloadJson = SourceItemPayloadMapper.toJson(payload),
        sourceName = sourceName.name,
        sourceKey = sourceKey,
        collectedAtUtc = collectedAt.toEpochMilli(),
    )

internal fun SourceItemEntity.toDomain(): SourceItem {
    val type = ItemType.valueOf(itemType)
    return SourceItem(
        rawId = rawId,
        startAt = Instant.ofEpochMilli(startAtUtc),
        endAt = endAtUtc?.let(Instant::ofEpochMilli),
        timeZoneId = ZoneId.of(timeZoneId),
        payload = SourceItemPayloadMapper.fromJson(type, payloadJson),
        sourceName = SourceName.valueOf(sourceName),
        sourceKey = sourceKey,
        collectedAt = Instant.ofEpochMilli(collectedAtUtc),
    )
}
