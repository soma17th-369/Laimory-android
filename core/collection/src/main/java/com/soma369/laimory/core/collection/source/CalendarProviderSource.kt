package com.soma369.laimory.core.collection.source

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.provider.CalendarContract
import com.soma369.laimory.core.collection.collector.CalendarAllDayPolicy
import com.soma369.laimory.core.collection.collector.CalendarDescriptionPolicy
import com.soma369.laimory.core.collection.collector.CalendarInstanceIdentity
import com.soma369.laimory.core.collection.collector.resolveCalendarQuery
import com.soma369.laimory.core.domain.model.collection.CalendarPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.source.CalendarSource
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * CalendarContract 기반 개인 일정 조회([CalendarSource] 구현).
 *
 * 사용자가 소유한(owner-access) 캘린더의 일정만 대상으로 한다 — 구독/공휴일/공유 읽기전용 캘린더는 제외한다.
 * 반복 일정은 인스턴스 단위로 펼쳐진다.
 *
 * 권한이 없어 `SecurityException` 이 나면 빈 목록을 반환한다. 그 밖의 실패는 삼키지 않고 올린다. 권한 확인을
 * 통과한 뒤의 null 커서도 실제 조회 실패로 본다([resolveCalendarQuery]).
 *
 * 시각은 캘린더 앱에 보이는 그대로(기기 시간대 기준) 둔다. 시간 지정 일정은 BEGIN/END 를 실제 시각으로 쓰고,
 * 일정의 원본 시간대(EVENT_TIMEZONE)는 쓰지 않는다. 종일 일정은 날짜 표기인 BEGIN/END 를
 * [CalendarAllDayPolicy] 로 기기 시간대 자정으로 바꾼다. `timeZoneId` 는 모두 기기 시간대다.
 *
 * 조회는 blocking I/O 이므로 [Dispatchers.IO] 에서 수행한다.
 */
internal class CalendarProviderSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : CalendarSource {
        /**
         * Instances 조회 구간은 [window] 를 앞뒤로 [QUERY_MARGIN] 만큼 넓힌다.
         *
         * 종일 일정의 BEGIN/END 는 UTC 자정 날짜 표기라 provider 는 그 값으로 구간을 비교한다. 기기 시간대
         * 자정으로 바꾼 뒤에야 창과 겹치는 일정(KST 15일 종일 = 원본 15일 09:00 KST 시작)이 창 끝(15일 06:00)에서
         * 잘려 나간다. 넓혀 읽고 바꾼 시각으로 [RecordDateWindow.contains] 를 다시 거른다.
         */
        override suspend fun eventsIn(window: RecordDateWindow): List<SourceItem> =
            withContext(Dispatchers.IO) {
                val calendarIds = queryPersonalCalendarIds()
                if (calendarIds.isEmpty()) return@withContext emptyList()

                val zone = ZoneId.systemDefault()
                queryInstances(
                    startMillis = window.start.minus(QUERY_MARGIN).toEpochMilli(),
                    endMillis = window.end.plus(QUERY_MARGIN).toEpochMilli(),
                    calendarIds = calendarIds,
                    zone = zone,
                    readAt = Instant.now(),
                ).filter(window::contains)
            }

        /**
         * provider 는 일정·캘린더가 바뀌면 [CalendarContract.CONTENT_URI] 로 알린다. 동기화가 한꺼번에 여러 번
         * 알릴 수 있어 처리 중에 쌓인 신호는 하나로 합친다.
         *
         * 등록이 거부되면(권한 없이 provider 를 볼 수 없는 기기 등) 신호 없이 끝난다. 갱신은 복귀·권한 변화로도 오므로
         * 화면을 막지 않는다.
         */
        override fun changes(): Flow<Unit> =
            callbackFlow {
                val observer =
                    object : ContentObserver(null) {
                        override fun onChange(selfChange: Boolean) {
                            trySend(Unit)
                        }
                    }
                val isRegistered =
                    runCatching {
                        context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
                    }.onFailure { e ->
                        Logger.w(LogDomain.COLLECTION, "캘린더 변경 관찰 등록 실패: ${e::class.simpleName}")
                    }.isSuccess
                if (!isRegistered) {
                    close()
                    return@callbackFlow
                }
                awaitClose { context.contentResolver.unregisterContentObserver(observer) }
            }.conflate()

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
            zone: ZoneId,
            readAt: Instant,
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

            return cursor.use { readInstances(it, zone, readAt) }
        }

        private fun readInstances(
            cursor: Cursor,
            zone: ZoneId,
            readAt: Instant,
        ): List<SourceItem> {
            val eventIdColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val beginColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val titleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val descriptionColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
            val locationColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val allDayColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            val items = ArrayList<SourceItem>(cursor.count)
            while (cursor.moveToNext()) {
                val eventId = cursor.getLong(eventIdColumn)
                val begin = cursor.getLong(beginColumn)
                val end = cursor.getLong(endColumn)
                val allDay = cursor.getInt(allDayColumn) == 1
                items.add(
                    SourceItem(
                        rawId = CalendarInstanceIdentity.rawIdOf(eventId, begin),
                        startAt = toInstant(begin, allDay, zone),
                        endAt = toInstant(end, allDay, zone),
                        timeZoneId = zone,
                        payload =
                            CalendarPayload(
                                title = cursor.getString(titleColumn).orEmpty(),
                                description = CalendarDescriptionPolicy.limit(cursor.getString(descriptionColumn)),
                                locationText = cursor.getString(locationColumn),
                                allDay = allDay,
                            ),
                        sourceName = SourceName.CALENDAR_PROVIDER,
                        sourceKey = CalendarInstanceIdentity.sourceKeyOf(eventId, begin),
                        collectedAt = readAt,
                    ),
                )
            }
            return items
        }

        /** 시간 지정 일정의 BEGIN/END 는 실제 시각 그대로, 종일 일정은 날짜 표기라 [zone] 자정으로 바꾼다. */
        private fun toInstant(
            millis: Long,
            allDay: Boolean,
            zone: ZoneId,
        ): Instant = if (allDay) CalendarAllDayPolicy.toZonedMidnight(millis, zone) else Instant.ofEpochMilli(millis)

        private companion object {
            /** 종일 일정의 UTC 자정 표기와 기기 시간대 자정 사이의 차이(최대 ±14시간)를 덮는 조회 여유. */
            val QUERY_MARGIN: Duration = Duration.ofDays(1)

            val INSTANCE_PROJECTION =
                arrayOf(
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.DESCRIPTION,
                    CalendarContract.Instances.EVENT_LOCATION,
                    CalendarContract.Instances.ALL_DAY,
                )
        }
    }
