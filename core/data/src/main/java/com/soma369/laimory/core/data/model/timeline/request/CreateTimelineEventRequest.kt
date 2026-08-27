package com.soma369.laimory.core.data.model.timeline.request

import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Event 생성 전용 exact-shape JSON.
 *
 * 전역 Json 은 `explicitNulls = false` 라 값이 null 인 필수 키가 통째로 빠진다. 서버는 `subtitle`·
 * `endAt` 을 **키는 필수, 값은 nullable** 로 요구하므로(누락 400) 여기서도 [JsonObject] 를 직접
 * 조립한다.
 *
 * 사진 payload 는 수정과 **같은 함수**를 쓴다 — 모양이 갈라지면 한쪽만 서버 계약을 따라가고
 * 다른 쪽은 조용히 400 이 된다.
 */
internal fun CreateTimelineEventCommand.toRequestJson(): JsonObject =
    buildJsonObject {
        put("eventType", JsonPrimitive(eventType.name))
        put("title", JsonPrimitive(title))
        put("subtitle", subtitle.toJsonStringOrNull())
        put("startAt", JsonPrimitive(startAt.toApiDateTime()))
        put("endAt", endAt?.let { JsonPrimitive(it.toApiDateTime()) } ?: JsonNull)
        // 누락·null·blank 가 모두 메모 없음이라 빈 값이면 키를 넣지 않는다.
        memo?.takeIf(String::isNotBlank)?.let { put("memo", JsonPrimitive(it)) }
        if (photosToAdd.isNotEmpty()) {
            put("photosToAdd", JsonArray(photosToAdd.map { it.toRequestJson() }))
        }
    }
