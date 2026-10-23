package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.CalendarAllDayRecordPolicy
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.source.CalendarSource
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기록 창의 캘린더 일정을 시스템 캘린더에서 읽는다. 저장하지 않으므로 홈 카드·상세·전송이 모두 이것으로 읽는다.
 *
 * **창을 지금으로 자르지 않는다.** 캘린더 원본을 그대로 보고 그대로 보낸다 — 기록 창이 오늘 06:00 ~ 익일 06:00 이면
 * 아직 시작하지 않은 오늘 일정도 들어간다. 서버로 보내는 기록 창(`timelineWindow`)도 고른 범위 그대로다.
 *
 * 권한이 없으면 빈 목록이고, 조회 실패는 예외로 올린다([CalendarSource.eventsIn]).
 */
@Singleton
class GetCalendarEventsInWindowUseCase
    @Inject
    constructor(
        private val calendarSource: CalendarSource,
    ) {
        /**
         * [recordDate] 기록에 넣을 일정. 시간 지정 일정은 [window] 와 겹치면, 종일 일정은 그 날짜면 들어간다
         * ([CalendarAllDayRecordPolicy]). 기록 창은 늘 [recordDate] 당일에 시작하므로 그날 종일 일정은 창과도 겹친다.
         */
        suspend operator fun invoke(
            window: RecordDateWindow,
            recordDate: LocalDate,
            zone: ZoneId,
        ): List<SourceItem> =
            calendarSource
                .eventsIn(window)
                .filter { CalendarAllDayRecordPolicy.belongsTo(it, recordDate, zone) }

        /** 기록 날짜가 없는 조회(수집 실험실의 최근 한 달). 종일 일정도 겹침으로만 거른다. */
        suspend operator fun invoke(window: RecordDateWindow): List<SourceItem> = calendarSource.eventsIn(window)
    }
