package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.source.CalendarSource
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기록 창의 캘린더 일정을 시스템 캘린더에서 읽는다. 저장하지 않으므로 홈 카드·상세·전송이 모두 이것으로 읽는다.
 *
 * **아직 시작하지 않은 일정은 넣지 않는다.** 기록 창은 오늘 06:00 ~ 익일 06:00 처럼 미래를 걸치는데, 창 그대로
 * 읽으면 오후에 만든 기록에 저녁 약속이 "있었던 일" 로 실린다. 그래서 읽는 끝을 지금으로 자른다. 이미 시작해
 * 지금을 넘겨 이어지는 일정은 들어온다.
 *
 * 권한이 없으면 빈 목록이고, 조회 실패는 예외로 올린다([CalendarSource.eventsIn]).
 */
@Singleton
class GetCalendarEventsInWindowUseCase
    @Inject
    constructor(
        private val calendarSource: CalendarSource,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(window: RecordDateWindow): List<SourceItem> {
            val readable = window.untilNow(clock.instant()) ?: return emptyList()
            return calendarSource.eventsIn(readable)
        }

        /** 창의 끝을 [now] 로 자른다. 창 전체가 미래면 null 이다. */
        private fun RecordDateWindow.untilNow(now: Instant): RecordDateWindow? =
            when {
                end <= now -> this
                start < now -> copy(end = now)
                else -> null
            }
    }
