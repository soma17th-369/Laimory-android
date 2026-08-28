package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.TimelineEventUpdateException
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
 *
 * 실패는 수정과 **같은 사유**([TimelineEventUpdateException.Reason])로 옮긴다. 두 요청이 같은
 * 편집기를 쓰므로 화면이 한 벌의 분기로 반응해야 한다.
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
            try {
                execute {
                    try {
                        val created = repository.createEvent(command)
                        refreshTimelineRecordSession(command.recordDate, repository, sessionRepository)
                        created
                    } catch (exception: ApiException) {
                        // 화면이 다룰 수 있는 실패는 의미로 바꿔 올린다. 그냥 두면 404 가 공통 정책에서
                        // HandledException 이 돼 편집기가 아무 반응 없이 그대로 남는다.
                        val reason = TimelineEventUpdateException.reasonOf(exception.errorCode) ?: throw exception
                        throw TimelineEventUpdateException(reason, exception)
                    }
                }
            } catch (exception: TimelineEventUpdateException) {
                Result.failure(exception)
            }
    }
