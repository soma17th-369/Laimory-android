package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.DailyRecordReadOutcome
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import javax.inject.Inject
import javax.inject.Singleton

/** DailyRecord 한 건을 조회하고 미존재·비소유 `-404`를 조회 outcome으로 표현한다. */
@Singleton
class GetDailyRecordUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(dailyRecordId: Long): Result<DailyRecordReadOutcome> =
            execute {
                try {
                    DailyRecordReadOutcome.Record(repository.getDailyRecord(dailyRecordId))
                } catch (exception: ApiException) {
                    when (exception.errorCode) {
                        RECORD_UNAVAILABLE_ERROR_CODE -> DailyRecordReadOutcome.Unavailable
                        else -> throw exception
                    }
                }
            }

        private companion object {
            const val RECORD_UNAVAILABLE_ERROR_CODE = -404
        }
    }
