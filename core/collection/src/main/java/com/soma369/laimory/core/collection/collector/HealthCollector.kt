package com.soma369.laimory.core.collection.collector

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.soma369.laimory.core.domain.collector.Collector
import com.soma369.laimory.core.domain.model.collection.HealthPayload
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * Health Connect 기반 건강 데이터 수집기. 초기 범위는 걸음수(일별 집계)와 수면(세션)이다.
 *
 * 걸음수는 하루 단위로 집계해 day-bucket 1건, 수면은 세션마다 1건의 [SourceItem] 으로 만든다.
 * `value` 는 계산 가능한 raw 숫자(걸음수 / 수면 분), `unit` 은 canonical(`count` / `minute`)로 저장한다 —
 * "10145보" 같은 표시 문자열은 UI/업로드 projection 에서 렌더한다.
 *
 * Health Connect 미설치/미지원이면(연동 불가) 빈 목록을 반환한다. 권한(READ_STEPS/READ_SLEEP) 확인/요청은
 * 수집기 밖(UI 계층)의 책임이며([Collector] 계약), 권한이 없어 읽기가 실패하면 빈 목록을 반환한다.
 * sourceKey 는 걸음수 `STEPS:{날짜}`, 수면 `SLEEP:{record id}` 로 재스캔에도 인스턴스별로 유일하다.
 */
internal class HealthCollector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Collector {
        override val itemType: ItemType = ItemType.HEALTH

        override suspend fun collect(): List<SourceItem> {
            if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
                Logger.w(LogDomain.COLLECTION, "Health Connect 사용 불가(미설치/업데이트 필요)")
                return emptyList()
            }
            val client = HealthConnectClient.getOrCreate(context)

            val zone = ZoneId.systemDefault()
            val collectedAt = Instant.now()
            val startDate = LocalDate.now(zone).minusMonths(COLLECTION_MONTHS)
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = LocalDateTime.now(zone)

            /*
             * 권한 미허용 시 read 가 SecurityException 을 던진다 — 계약대로 빈 목록이다.
             *
             * 실제 조회 실패는 삼키지 않고 올린다. 자동 수집은 "권한이 없어 건너뜀" 과 "긁다가
             * 실패함" 을 구분해야 하는데, 둘 다 빈 목록이 되면 호출 경계에서 나눌 수 없다.
             * 수동 수집 경로는 예외를 기존처럼 상위에서 처리한다.
             */
            return try {
                collectSteps(client, startDateTime, endDateTime, zone, collectedAt) +
                    collectSleep(client, startDateTime, endDateTime, zone, collectedAt)
            } catch (_: SecurityException) {
                Logger.w(LogDomain.COLLECTION, "건강 데이터 읽기 권한 없음")
                emptyList()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 예외 메시지에 건강 값이 실릴 수 있어 종류만 남긴다.
                Logger.w(LogDomain.COLLECTION, "건강 데이터 수집 실패: ${e.javaClass.simpleName}")
                throw e
            }
        }

        /** 걸음수를 하루 단위로 집계해 day-bucket 당 1건으로 만든다. 데이터 없는 날은 건너뛴다. */
        private suspend fun collectSteps(
            client: HealthConnectClient,
            startDateTime: LocalDateTime,
            endDateTime: LocalDateTime,
            zone: ZoneId,
            collectedAt: Instant,
        ): List<SourceItem> {
            val buckets =
                client.aggregateGroupByPeriod(
                    AggregateGroupByPeriodRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startDateTime, endDateTime),
                        timeRangeSlicer = Period.ofDays(1),
                    ),
                )
            return buckets.mapNotNull { bucket ->
                val steps = bucket.result[StepsRecord.COUNT_TOTAL] ?: return@mapNotNull null
                SourceItem(
                    rawId = UUID.randomUUID().toString(),
                    startAt = bucket.startTime.atZone(zone).toInstant(),
                    endAt = bucket.endTime.atZone(zone).toInstant(),
                    timeZoneId = zone,
                    payload =
                        HealthPayload(
                            metric = HealthPayload.Metric.STEPS,
                            value = steps.toDouble(),
                            unit = UNIT_COUNT,
                        ),
                    sourceName = SourceName.HEALTH_CONNECT,
                    sourceKey = "STEPS:${bucket.startTime.toLocalDate()}",
                    collectedAt = collectedAt,
                )
            }
        }

        /** 수면 세션마다 1건으로 만든다. value 는 세션 길이(분). */
        private suspend fun collectSleep(
            client: HealthConnectClient,
            startDateTime: LocalDateTime,
            endDateTime: LocalDateTime,
            zone: ZoneId,
            collectedAt: Instant,
        ): List<SourceItem> {
            val sessions =
                client
                    .readRecords(
                        ReadRecordsRequest(
                            recordType = SleepSessionRecord::class,
                            timeRangeFilter =
                                TimeRangeFilter.between(
                                    startDateTime.atZone(zone).toInstant(),
                                    endDateTime.atZone(zone).toInstant(),
                                ),
                        ),
                    ).records
            return sessions.map { session ->
                val minutes = Duration.between(session.startTime, session.endTime).toMinutes()
                SourceItem(
                    rawId = UUID.randomUUID().toString(),
                    startAt = session.startTime,
                    endAt = session.endTime,
                    timeZoneId = session.startZoneOffset ?: zone,
                    payload =
                        HealthPayload(
                            metric = HealthPayload.Metric.SLEEP,
                            value = minutes.toDouble(),
                            unit = UNIT_MINUTE,
                        ),
                    sourceName = SourceName.HEALTH_CONNECT,
                    sourceKey = "SLEEP:${session.metadata.id}",
                    collectedAt = collectedAt,
                )
            }
        }

        private companion object {
            /** 오늘 기준 조회 구간(개월). 라이프로그라 과거 구간을 본다. */
            const val COLLECTION_MONTHS = 1L

            /** 걸음수 canonical 단위. 표시("보")는 UI/업로드에서 렌더한다. */
            const val UNIT_COUNT = "count"

            /** 수면 canonical 단위. 표시("분")는 UI/업로드에서 렌더한다. */
            const val UNIT_MINUTE = "minute"
        }
    }
