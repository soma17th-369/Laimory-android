package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캘린더 표시 월의 기록 날짜와 감정을 조회한다.
 *
 * 전체 조회([GetDailyRecordsUseCase])와 달리 한 달치 최소 정보만 받는다. 전체 조회는 홈의 지난 기록
 * 목록이 계속 쓰므로 이 UseCase 가 대체하지 않는다.
 */
@Singleton
class GetMonthlyDailyRecordsUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(month: YearMonth): Result<List<MonthlyDailyRecord>> =
            execute { repository.getMonthlyDailyRecords(month) }
    }
