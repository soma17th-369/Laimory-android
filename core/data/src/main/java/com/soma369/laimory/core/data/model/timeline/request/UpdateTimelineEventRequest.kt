package com.soma369.laimory.core.data.model.timeline.request

import com.soma369.laimory.core.domain.model.timeline.TimelineEventPhotoAddition
import com.soma369.laimory.core.domain.model.timeline.TimelineEventUpdateField
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Event PATCH 전용 exact-shape JSON.
 *
 * 전역 Json은 `explicitNulls = false`이므로 필수 nullable 키와 optional presence를 동시에 표현할 수 없다.
 * 이 요청만 [JsonObject]를 직접 조립해 서버의 키 존재 계약을 보존한다.
 */
internal fun UpdateTimelineEventCommand.toRequestJson(): JsonObject =
    buildJsonObject {
        put("title", JsonPrimitive(title))
        put("subtitle", subtitle.toJsonStringOrNull())
        put("startAt", JsonPrimitive(startAt.toApiDateTime()))
        put("endAt", endAt?.let { JsonPrimitive(it.toApiDateTime()) } ?: JsonNull)
        eventType?.let { put("eventType", JsonPrimitive(it.name)) }

        when (val memoField = memo) {
            TimelineEventUpdateField.Unchanged -> Unit
            is TimelineEventUpdateField.Value -> put("memo", memoField.value.toJsonStringOrNull())
        }
        when (val photoField = photosToAdd) {
            TimelineEventUpdateField.Unchanged -> Unit
            is TimelineEventUpdateField.Value -> put("photosToAdd", JsonArray(photoField.value.map { it.toRequestJson() }))
        }
    }

internal fun TimelineEventPhotoAddition.toRequestJson(): JsonObject =
    buildJsonObject {
        put("rawId", JsonPrimitive(rawId))
        startAt?.let { put("startAt", JsonPrimitive(it.toApiDateTime())) }
        endAt?.let { put("endAt", JsonPrimitive(it.toApiDateTime())) }
        put(
            "payload",
            buildJsonObject {
                put("filename", JsonPrimitive(filename))
                put("clientPhotoUri", JsonPrimitive(clientPhotoUri))
                latitude?.let { put("latitude", JsonPrimitive(it)) }
                longitude?.let { put("longitude", JsonPrimitive(it)) }
            },
        )
    }

internal fun String?.toJsonStringOrNull() = this?.let(::JsonPrimitive) ?: JsonNull

internal fun LocalDateTime.toApiDateTime(): String = API_DATE_TIME_FORMATTER.format(this)

private val API_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
