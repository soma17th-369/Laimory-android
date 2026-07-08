package com.soma369.laimory.feature.timeline.testexport

import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.LocationPayload
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 임시 테스트 전용(삭제 예정) — 하루치 수집 데이터를 사람 확인용 JSON 으로 만든다.
 *
 * 절단면: `recordDate` / `recordTimeZone` / `sourceItems[]`.
 * 각 아이템은 `rawId` / `itemType` / `startAt` / `endAt` / `payload` 로 표현하고,
 * `startAt`·`endAt` 은 아이템의 `timeZoneId` 기준 로컬 datetime(오프셋 없음)으로 렌더한다.
 * 서버 계약(#120)이 확정되면 그 스키마에 맞춰 갱신한다.
 */
internal object DayBundleJson {
    /**
     * [date] 하루(창 안) [items] 를 JSON 문자열로 만든다.
     * PHOTO 아이템은 `photos/` 에 올라갈 파일명을 [photoFileName] 로 받아 링크한다(없으면 null).
     */
    fun build(
        date: LocalDate,
        zone: ZoneId,
        items: List<SourceItem>,
        photoFileName: (SourceItem) -> String?,
    ): String {
        val root =
            JSONObject().apply {
                put("recordDate", date.toString())
                put("recordTimeZone", zone.id)
                val arr = JSONArray()
                items.forEach { item -> arr.put(itemJson(item, photoFileName(item))) }
                put("sourceItems", arr)
            }
        return root.toString(2)
    }

    private fun itemJson(
        item: SourceItem,
        photoFile: String?,
    ): JSONObject =
        JSONObject().apply {
            put("rawId", item.rawId)
            put("itemType", item.itemType.name)
            put("startAt", localDateTime(item.startAt, item.timeZoneId))
            put("endAt", item.endAt?.let { localDateTime(it, item.timeZoneId) } ?: JSONObject.NULL)
            put("payload", payloadJson(item.payload, photoFile))
        }

    /** [instant] 를 [zone] 기준 로컬 datetime 문자열(오프셋 없음)로 렌더한다. */
    private fun localDateTime(
        instant: Instant,
        zone: ZoneId,
    ): String = instant.atZone(zone).toLocalDateTime().toString()

    private fun payloadJson(
        payload: SourceItemPayload,
        photoFile: String?,
    ): JSONObject =
        JSONObject().apply {
            when (payload) {
                is PhotoPayload -> {
                    put("fileName", payload.fileName)
                    put("photoFile", photoFile ?: JSONObject.NULL)
                    put("latitude", payload.latitude ?: JSONObject.NULL)
                    put("longitude", payload.longitude ?: JSONObject.NULL)
                    put("description", payload.description ?: JSONObject.NULL)
                }
                is CalendarPayload -> {
                    put("title", payload.title)
                    put("description", payload.description ?: JSONObject.NULL)
                    put("locationText", payload.locationText ?: JSONObject.NULL)
                    put("allDay", payload.allDay)
                }
                is HealthPayload -> {
                    put("metric", payload.metric.name)
                    put("value", healthDisplayValue(payload))
                }
                is LocationPayload -> {
                    put("latitude", payload.latitude)
                    put("longitude", payload.longitude)
                }
                is MovementPayload -> {
                    put("distanceMeters", payload.distanceMeters)
                    put("transports", payload.transports.name)
                }
                is NotificationPayload -> {
                    put("appName", payload.appName)
                    put("title", payload.title ?: JSONObject.NULL)
                    put("text", payload.text ?: JSONObject.NULL)
                }
            }
        }

    /** 걸음수·수면은 표시 문자열로 보낸다: STEPS "8421보" / SLEEP "480분". */
    private fun healthDisplayValue(payload: HealthPayload): String {
        val amount = payload.value.toInt()
        return when (payload.metric) {
            HealthPayload.Metric.STEPS -> "${amount}보"
            HealthPayload.Metric.SLEEP -> "${amount}분"
        }
    }
}
