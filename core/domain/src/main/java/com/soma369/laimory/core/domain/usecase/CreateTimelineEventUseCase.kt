package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.CreateTimelineEventCommand
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 하루 기록에 새 Event 를 만든다.
 *
 * 만든 뒤 하루 기록을 **서버에서 다시 읽어** 세션을 갈아 끼운다. 응답 하나를 목록에 끼워 넣는
 * 방식은 클라이언트가 서버의 계산을 흉내 내는 것이라, 흉내가 틀리면 화면만 조용히 어긋난다.
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
                refreshTimelineRecordSession(command.recordDate, repository, sessionRepository)
                created
            }
    }
