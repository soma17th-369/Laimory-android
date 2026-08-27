package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UpdateDailyRecordEmotionOutcome {
    /** 서버가 감정을 교체했다. 같은 값 재요청도 여기로 온다(멱등). */
    data object Updated : UpdateDailyRecordEmotionOutcome

    /**
     * `-1020` — 아직 SAVED 가 아니다.
     *
     * DRAFT 의 최초 감정은 작성 완료([CompleteDailyRecordUseCase])가 정하므로 이 경로로 오면 안 된다.
     * 화면이 진입을 막는 것이 1차 방어이고, 그래도 도달했을 때 조용히 성공으로 보이지 않게 구분해 둔다.
     */
    data object NotSaved : UpdateDailyRecordEmotionOutcome

    /** `-404` — 해당 날짜의 하루 기록이 더 이상 존재하지 않는다. */
    data object RecordUnavailable : UpdateDailyRecordEmotionOutcome
}

/**
 * 저장된 하루 기록의 감정을 교체한다.
 *
 * `-1020` 은 [BaseUseCase] 공통 정책(401·404·5xx)이 다루지 않아 그대로 통과하므로 여기서 결과로
 * 바꿔 화면이 문구를 정하게 한다. `-404` 는 공통 정책이 메시지를 내지만, 화면이 기록 소실을 알고
 * 목록으로 돌아갈 수 있어야 해서 함께 구분한다.
 */
@Singleton
class UpdateDailyRecordEmotionUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(
            recordDate: LocalDate,
            emotion: TimelineEmotion,
        ): Result<UpdateDailyRecordEmotionOutcome> =
            execute {
                try {
                    repository.updateDailyRecordEmotion(recordDate, emotion)
                    UpdateDailyRecordEmotionOutcome.Updated
                } catch (exception: ApiException) {
                    when (exception.errorCode) {
                        RECORD_NOT_SAVED_ERROR_CODE -> UpdateDailyRecordEmotionOutcome.NotSaved
                        RECORD_UNAVAILABLE_ERROR_CODE -> UpdateDailyRecordEmotionOutcome.RecordUnavailable
                        else -> throw exception
                    }
                }
            }

        private companion object {
            const val RECORD_NOT_SAVED_ERROR_CODE = -1020
            const val RECORD_UNAVAILABLE_ERROR_CODE = -404
        }
    }
