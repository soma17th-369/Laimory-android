package com.soma369.laimory.core.domain.usecase.tutorial

import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
import com.soma369.laimory.core.domain.model.timeline.CalendarAllDayRecordPolicy
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.model.tutorial.RecommendedRecordDate
import com.soma369.laimory.core.domain.source.CalendarSource
import com.soma369.laimory.core.domain.source.PhotoSource
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 첫 타임라인을 만들어 볼 날을 추천한다 — **어제까지 한 달 중 직접 찍은 사진이 많은 날** 최대 [MAX_COUNT] 개.
 *
 * 위치·알림은 설치한 뒤부터 쌓여 새 사용자의 지난 날에는 없다. 사진과 일정은 설치 전 것도 읽을 수 있어, 사진이 많은 날을
 * 고르면 첫날에도 내용이 채워진 하루로 만들어 볼 수 있다.
 *
 * - 오늘은 넣지 않는다. 하루가 끝나지 않았고, 홈은 이미 오늘을 보고 있다.
 * - 날짜마다 [range] 를 얹은 기록 창으로 센다. 그 날을 골랐을 때 홈이 세는 창과 같다.
 * - 순위는 직접 찍은 사진 수, 같으면 일정 수, 그래도 같으면 최근 날이 앞이다.
 * - 직접 찍은 사진이 없는 날과 [excludedDates](이미 기록이 있는 날)는 뺀다.
 *
 * 사진·일정은 한 달 전체를 한 번씩 읽어 날짜별로 나눈다. 권한이 없으면 빈 목록이 와 추천이 비거나 일정 수가 0 이 된다.
 * 일정 읽기가 실패해도 추천은 사진으로 하므로 일정 수를 0 으로 두고 계속한다.
 */
@Singleton
class RecommendTutorialDatesUseCase
    @Inject
    constructor(
        private val photoSource: PhotoSource,
        private val calendarSource: CalendarSource,
    ) {
        /**
         * @param retentionDays 날짜 피커에서 고를 수 있는 일수(오늘 포함). 한 달보다 짧으면 그 안에서만 고른다.
         */
        suspend operator fun invoke(
            today: LocalDate,
            zone: ZoneId,
            range: DefaultRecordRange,
            retentionDays: Int?,
            excludedDates: Set<LocalDate>,
        ): List<RecommendedRecordDate> {
            val lookbackDays = minOf(LOOKBACK_DAYS, retentionDays ?: LOOKBACK_DAYS)
            val windows =
                (1 until lookbackDays)
                    .map { today.minusDays(it.toLong()) }
                    .filterNot(excludedDates::contains)
                    .mapNotNull { date -> range.windowOn(date, zone)?.let { date to it } }
            if (windows.isEmpty()) return emptyList()

            val span = RecordDateWindow(start = windows.minOf { it.second.start }, end = windows.maxOf { it.second.end })
            val photos = photoSource.capturedPhotosIn(span)
            if (photos.isEmpty()) return emptyList()
            val events = readEvents(span)

            return windows
                .mapNotNull { (date, window) -> recommendationOf(date, window, photos, events, zone) }
                .sortedWith(
                    compareByDescending(RecommendedRecordDate::capturedPhotoCount)
                        .thenByDescending(RecommendedRecordDate::calendarEventCount)
                        .thenByDescending(RecommendedRecordDate::date),
                ).take(MAX_COUNT)
        }

        private suspend fun readEvents(span: RecordDateWindow): List<SourceItem> =
            try {
                calendarSource.eventsIn(span)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }

        private fun recommendationOf(
            date: LocalDate,
            window: RecordDateWindow,
            photos: List<PhotoCandidate>,
            events: List<SourceItem>,
            zone: ZoneId,
        ): RecommendedRecordDate? {
            val dayPhotos = photos.filter { it.takenAt >= window.start && it.takenAt < window.end }
            if (dayPhotos.isEmpty()) return null
            return RecommendedRecordDate(
                date = date,
                capturedPhotoCount = dayPhotos.size,
                calendarEventCount = events.count { window.contains(it) && CalendarAllDayRecordPolicy.belongsTo(it, date, zone) },
                photoUris =
                    dayPhotos
                        .sortedBy(PhotoCandidate::takenAt)
                        .evenlySampled(RecommendedRecordDate.PREVIEW_PHOTO_COUNT)
                        .map(PhotoCandidate::contentUri),
            )
        }

        /** 차례를 지킨 채 [count] 장을 고르게 뽑는다. 앞쪽 몇 장만 자르면 많이 찍은 날의 아침만 보인다. */
        private fun <T> List<T>.evenlySampled(count: Int): List<T> = if (size <= count) this else List(count) { this[it * size / count] }

        /** [date] 에 범위를 얹은 기록 창. 범위가 깨져 시작이 종료보다 늦으면 null — 그 날은 추천하지 않는다. */
        private fun DefaultRecordRange.windowOn(
            date: LocalDate,
            zone: ZoneId,
        ): RecordDateWindow? {
            val start = date.atTime(startTime).atZone(zone).toInstant()
            val end = date.plusDays(if (endsNextDay) 1 else 0).atTime(endTime).atZone(zone).toInstant()
            return if (start < end) RecordDateWindow(start, end) else null
        }

        companion object {
            /** 추천하는 날 수. */
            const val MAX_COUNT = 3

            /** 거슬러 볼 일수(오늘 포함). 오늘을 빼면 어제부터 29일이다. */
            const val LOOKBACK_DAYS = 30
        }
    }
