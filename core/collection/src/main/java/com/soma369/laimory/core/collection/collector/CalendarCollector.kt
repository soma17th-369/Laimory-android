package com.soma369.laimory.core.collection.collector

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.soma369.laimory.core.domain.collector.Collector
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * CalendarContract 기반 개인 일정 수집기.
 *
 * 사용자가 소유한(owner-access) 캘린더의 일정만 대상으로 한다 — 구독/공휴일/공유 읽기전용 캘린더는 제외한다.
 * 오늘 기준 최근 [COLLECTION_MONTHS]개월 구간의 인스턴스를 수집하며, 반복 일정은 인스턴스 단위로 펼쳐진다.
 *
 * 권한(READ_CALENDAR) 확인/요청은 수집기 밖(UI 계층)의 책임이다([Collector] 계약) — 권한이 없어
 * `SecurityException` 이 나면 빈 목록을 반환한다. 그 밖의 실패는 삼키지 않고 올린다. 권한 확인을 통과한
 * 뒤의 null 커서도 실제 조회 실패로 본다([resolveCalendarQuery]) — 빈 목록으로 바꾸면 자동 수집이
 * "성공 0건" 으로 확정하고 최신성까지 갱신해, 실패 안내 없이 옛 데이터를 계속 쓴다.
 *
 * sourceKey 는 `eventId:begin` 으로 인스턴스마다 유일해, 반복 일정도 인스턴스별로 dedupe 된다.
 * 조회는 blocking I/O 이므로 [Dispatchers.IO] 에서 수행한다.
 */
internal class CalendarCollector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Collector {
        override val itemType: ItemType = ItemType.CALENDAR

        override suspend fun collect(): List<SourceItem> =
            withContext(Dispatchers.IO) {
                val calendarIds = queryPersonalCalendarIds()
                if (calendarIds.isEmpty()) return@withContext emptyList()

                val zone = ZoneId.systemDefault()
                val collectedAt = Instant.now()
                val startMillis = LocalDate.now(zone).minusMonths(COLLECTION_MONTHS).atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = collectedAt.toEpochMilli()
                queryInstances(startMillis, endMillis, calendarIds, collectedAt)
            }

        /** owner-access(사용자 소유) 캘린더의 id 만 모은다. 구독/공휴일/공유 캘린더는 접근 레벨이 낮아 제외된다. */
        private fun queryPersonalCalendarIds(): List<Long> {
            val cursor =
                resolveCalendarQuery(
                    label = "개인 캘린더 목록",
                    result =
                        runCatching {
                            context.contentResolver.query(
                                CalendarContract.Calendars.CONTENT_URI,
                                arrayOf(CalendarContract.Calendars._ID),
                                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} = ?",
                                arrayOf(CalendarContract.Calendars.CAL_ACCESS_OWNER.toString()),
                                null,
                            )
                        },
                ) ?: return emptyList()

            return cursor.use { c ->
                val idColumn = c.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                buildList { while (c.moveToNext()) add(c.getLong(idColumn)) }
            }
        }

        private fun queryInstances(
            startMillis: Long,
            endMillis: Long,
            calendarIds: List<Long>,
            collectedAt: Instant,
        ): List<SourceItem> {
            val uri =
                CalendarContract.Instances.CONTENT_URI
                    .buildUpon()
                    .apply {
                        ContentUris.appendId(this, startMillis)
                        ContentUris.appendId(this, endMillis)
                    }.build()
            val placeholders = calendarIds.joinToString(",") { "?" }
            val cursor =
                resolveCalendarQuery(
                    label = "일정 인스턴스",
                    result =
                        runCatching {
                            context.contentResolver.query(
                                uri,
                                INSTANCE_PROJECTION,
                                "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)",
                                calendarIds.map(Long::toString).toTypedArray(),
                                "${CalendarContract.Instances.BEGIN} ASC",
                            )
                        },
                ) ?: return emptyList()

            return cursor.use { readInstances(it, collectedAt) }
        }

        private fun readInstances(
            cursor: Cursor,
            collectedAt: Instant,
        ): List<SourceItem> {
            val eventIdColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val beginColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val titleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val descriptionColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
            val locationColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val allDayColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val timeZoneColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_TIMEZONE)

            val items = ArrayList<SourceItem>(cursor.count)
            while (cursor.moveToNext()) {
                val begin = cursor.getLong(beginColumn)
                val end = cursor.getLong(endColumn)
                val allDay = cursor.getInt(allDayColumn) == 1
                items.add(
                    SourceItem(
                        rawId = UUID.randomUUID().toString(),
                        startAt = Instant.ofEpochMilli(begin),
                        endAt = Instant.ofEpochMilli(end),
                        timeZoneId = cursor.getString(timeZoneColumn).toZoneIdOrDefault(allDay),
                        payload =
                            CalendarPayload(
                                title = cursor.getString(titleColumn).orEmpty(),
                                description = CalendarDescriptionPolicy.limit(cursor.getString(descriptionColumn)),
                                locationText = cursor.getString(locationColumn),
                                allDay = allDay,
                            ),
                        sourceName = SourceName.CALENDAR_PROVIDER,
                        // 인스턴스 단위 키 — 반복 일정도 발생 시각별로 유일하게 dedupe 된다.
                        sourceKey = "${cursor.getLong(eventIdColumn)}:$begin",
                        collectedAt = collectedAt,
                    ),
                )
            }
            return items
        }

        /** 종일 일정의 BEGIN/END 는 UTC 자정 기준이므로 UTC 로 둔다. 그 외는 이벤트 타임존, 파싱 실패/부재 시 기기 시간대. */
        private fun String?.toZoneIdOrDefault(allDay: Boolean): ZoneId =
            when {
                allDay -> ZoneId.of("UTC")
                isNullOrBlank() -> ZoneId.systemDefault()
                else -> runCatching { ZoneId.of(this) }.getOrDefault(ZoneId.systemDefault())
            }

        private companion object {
            /** 오늘 기준 조회 구간(개월). 라이프로그라 과거 구간을 본다 — 미래로 바꾸려면 이 창을 조정한다. */
            const val COLLECTION_MONTHS = 1L

            val INSTANCE_PROJECTION =
                arrayOf(
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.DESCRIPTION,
                    CalendarContract.Instances.EVENT_LOCATION,
                    CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.EVENT_TIMEZONE,
                )
        }
    }

/**
 * 캘린더 조회 결과를 권한 없음과 실제 실패로 가른다.
 *
 * - `SecurityException`: 권한 미허용. 수집기 계약대로 빈 결과(null)로 돌아간다.
 * - `null` 값: `ContentResolver.query` 는 provider 가 null 을 돌려주거나 죽었을 때도 null 을 낸다.
 *   권한 확인을 통과한 뒤의 null 은 **실제 조회 실패**라 예외로 올린다. 빈 목록으로 바꾸면 자동
 *   수집이 "성공 0건" 으로 확정하고 최신성까지 갱신해, 실패 안내 없이 옛 데이터를 계속 쓴다.
 * - 그 밖의 예외: 그대로 올린다.
 *
 * 로그에는 일정 제목이 실릴 수 있는 예외 메시지를 남기지 않는다.
 *
 * `Cursor` 를 다루지 않는 순수 판정이라 JVM 테스트로 검증한다 — 커서를 만들려면 계측 환경이 필요하다.
 */
internal fun <T> resolveCalendarQuery(
    label: String,
    result: Result<T?>,
): T? {
    val failure = result.exceptionOrNull()
    if (failure is SecurityException) {
        Logger.w(LogDomain.COLLECTION, "$label 읽기 권한 없음")
        return null
    }
    return result.getOrThrow() ?: error("$label 조회가 결과를 반환하지 않았습니다.")
}
