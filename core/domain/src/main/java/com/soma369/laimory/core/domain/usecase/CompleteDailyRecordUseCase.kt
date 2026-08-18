package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CompleteDailyRecordOutcome {
    /** 서버가 DRAFT를 SAVED로 확정했다. */
    data object Completed : CompleteDailyRecordOutcome

    /** `-1003` — 이미 SAVED. 응답 유실 뒤 재시도로 보고 작성 완료로 수렴한다. */
    data object AlreadySaved : CompleteDailyRecordOutcome

    /** `-404` — 해당 날짜의 하루 기록이 더 이상 존재하지 않는다. */
    data object RecordUnavailable : CompleteDailyRecordOutcome
}

/**
 * 선택한 하루 감정과 함께 서버에 하루 기록 작성 완료(DRAFT → SAVED)를 반영한다.
 *
 * 세션 인메모리 저장([SaveTimelineRecordUseCase])과 달리 서버 확정 API를 호출한다.
 * 확정·수렴·소실 어느 결과든 같은 날짜의 현재 초안 세션을 정리해 오래된 초안이 남지 않게 한다.
 *
 * 감정은 서버 계약상 필수라 호출부가 반드시 하나를 정해 넘긴다 — 미선택을 뜻하는 null 경로는 없다.
 */
@Singleton
class CompleteDailyRecordUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        private val sessionRepository: TimelineRecordSessionRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ): Result<CompleteDailyRecordOutcome> =
            execute {
                try {
                    repository.saveDailyRecord(recordDate, emotion)
                    clearSession(recordDate)
                    CompleteDailyRecordOutcome.Completed
                } catch (exception: ApiException) {
                    when (exception.errorCode) {
                        RECORD_SAVED_ERROR_CODE -> {
                            clearSession(recordDate)
                            CompleteDailyRecordOutcome.AlreadySaved
                        }
                        RECORD_UNAVAILABLE_ERROR_CODE -> {
                            clearSession(recordDate)
                            CompleteDailyRecordOutcome.RecordUnavailable
                        }
                        else -> throw exception
                    }
                }
            }

        private fun clearSession(recordDate: LocalDate) {
            if (sessionRepository.timeline.value?.recordDate == recordDate) {
                sessionRepository.clear()
            }
        }

        private companion object {
            const val RECORD_SAVED_ERROR_CODE = -1003
            const val RECORD_UNAVAILABLE_ERROR_CODE = -404
        }
    }
