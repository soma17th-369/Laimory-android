package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.TimelineRecordDeleteException
import com.soma369.laimory.core.domain.exception.toTimelineRecordDeleteExceptionOrNull
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 서버에서 DailyRecord 삭제가 성공한 뒤 현재 타임라인 세션을 비운다. */
@Singleton
class DeleteDailyRecordUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        private val sessionRepository: TimelineRecordSessionRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(dailyRecordId: Long): Result<Unit> =
            try {
                execute {
                    try {
                        repository.deleteDailyRecord(dailyRecordId)
                        if (sessionRepository.timeline.value?.dailyRecordId == dailyRecordId) {
                            sessionRepository.clear()
                        }
                    } catch (exception: ApiException) {
                        throw exception.toTimelineRecordDeleteExceptionOrNull() ?: exception
                    }
                }
            } catch (exception: TimelineRecordDeleteException) {
                Result.failure(exception)
            }
    }
