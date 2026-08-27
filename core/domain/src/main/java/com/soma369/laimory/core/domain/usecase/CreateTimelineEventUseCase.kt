package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 하루 기록에 새 Event 를 만든다.
 *
 * 생성 결과를 세션 타임라인에 바로 반영한다 — 화면이 다시 조회하지 않아도 목록이 새 이벤트를
 * 포함하고, 편집기에서 돌아온 직후 빈 화면을 보지 않는다. 수정([UpdateTimelineEventUseCase])과
 * 같은 방식이다.
 */
@Singleton
class CreateTimelineEventUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        private val sessionRepository: TimelineRecordSessionRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(command: CreateTimelineEventCommand): Result<TimelineEvent> =
            execute {
                val created = repository.createEvent(command)
                appendToSession(command.recordDate, created)
                created
            }

        /**
         * 세션 목록에 새 이벤트를 시각 순서대로 끼워 넣는다.
         *
         * 서버가 시각을 보낸 값 그대로 저장하고 겹침 보정을 하지 않으므로, 같은 시각이 있으면 그
         * 뒤에 둔다 — 목록이 서버 조회 순서와 어긋나지 않는다.
         */
        private fun appendToSession(
            recordDate: LocalDate,
            event: TimelineEvent,
        ) {
            val timeline = sessionRepository.timeline.value ?: return
            // 다른 날짜의 세션이면 건드리지 않는다 — 그 화면은 자기 날짜를 다시 조회해 맞춘다.
            if (timeline.recordDate != recordDate) return
            val merged = (timeline.events + event).sortedBy(TimelineEvent::startAt)
            sessionRepository.save(timeline.copy(events = merged))
        }
    }
