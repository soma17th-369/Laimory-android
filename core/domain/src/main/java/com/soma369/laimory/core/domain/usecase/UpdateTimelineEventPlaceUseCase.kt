package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.TimelineEventUpdateException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 전용 PUT으로 Event 장소를 바꾸고 서버 최신 Event를 현재 타임라인 세션에 반영한다.
 *
 * 후보를 고르면 그 후보의 좌표를 함께 넘겨 Event 좌표도 바꾼다. 직접 입력처럼 좌표가 없으면 null 로 보내고,
 * 서버는 지금 좌표를 그대로 둔다. 제목·부제·후보는 그대로다. 부제의 장소 자리가 새 장소로 채워지는 것은 표시
 * 단계의 일이다. 같은 값으로 다시 보내도 결과가 같아 응답을 못 받았으면 그대로 재시도해도 된다.
 */
@Singleton
class UpdateTimelineEventPlaceUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        private val sessionRepository: TimelineRecordSessionRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(
            timelineEventId: Long,
            place: String,
            latitude: Double? = null,
            longitude: Double? = null,
        ): Result<TimelineEvent> =
            try {
                execute {
                    // 좌표는 쌍이다. 한쪽만 보내면 서버가 400 으로 거절하므로 둘 다 없는 것으로 보낸다.
                    val hasCoordinate = latitude != null && longitude != null
                    try {
                        repository
                            .updateEventPlace(
                                timelineEventId = timelineEventId,
                                place = place.trim(),
                                latitude = latitude.takeIf { hasCoordinate },
                                longitude = longitude.takeIf { hasCoordinate },
                            )
                            .also(sessionRepository::replaceEvent)
                    } catch (exception: ApiException) {
                        val reason = exception.toUpdateReason() ?: throw exception
                        throw TimelineEventUpdateException(reason, exception)
                    }
                }
            } catch (exception: TimelineEventUpdateException) {
                Result.failure(exception)
            }

        private fun ApiException.toUpdateReason(): TimelineEventUpdateException.Reason? =
            when (errorCode) {
                INVALID_REQUEST_ERROR_CODE -> TimelineEventUpdateException.Reason.INVALID_REQUEST
                EVENT_UNAVAILABLE_ERROR_CODE -> TimelineEventUpdateException.Reason.EVENT_UNAVAILABLE
                else -> null
            }

        companion object {
            /** 서버가 받는 장소 이름 최대 길이. 화면이 확인 버튼을 막는 기준으로도 쓴다. */
            const val MAX_PLACE_LENGTH = 255

            private const val INVALID_REQUEST_ERROR_CODE = -400
            private const val EVENT_UNAVAILABLE_ERROR_CODE = -404
        }
    }
