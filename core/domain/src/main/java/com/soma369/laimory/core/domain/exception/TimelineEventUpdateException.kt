package com.soma369.laimory.core.domain.exception

/**
 * Event 수정 화면이 직접 복구하거나 안내해야 하는 기능 단위 실패.
 *
 * 서버 문자열 코드는 [UpdateTimelineEventUseCase][com.soma369.laimory.core.domain.usecase.UpdateTimelineEventUseCase]
 * 경계에서 이 의미로 변환하며 Presentation에는 노출하지 않는다.
 */
class TimelineEventUpdateException(
    val reason: Reason,
    override val cause: ApiException,
) : Exception(cause.message, cause) {
    enum class Reason {
        INVALID_REQUEST,
        PHOTO_LIMIT_EXCEEDED,
        EVENT_UNAVAILABLE,
        RECORD_ALREADY_SAVED,
        DATE_OPERATION_IN_PROGRESS,
    }
}
