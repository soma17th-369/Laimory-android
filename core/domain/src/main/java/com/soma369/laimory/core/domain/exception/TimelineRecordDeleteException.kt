package com.soma369.laimory.core.domain.exception

/**
 * 타임라인 Event 또는 DailyRecord 삭제 화면이 직접 안내하고 재시도할 수 있는 기능 오류.
 *
 * 서버 오류 코드는 UseCase 경계에서 [Reason]으로 변환해 Presentation에 노출하지 않는다.
 */
class TimelineRecordDeleteException(
    val reason: Reason,
    override val cause: ApiException,
) : Exception(cause.message, cause) {
    enum class Reason {
        TARGET_UNAVAILABLE,
        DATE_OPERATION_IN_PROGRESS,
        PHOTO_DELETE_FAILED,
    }
}

internal fun ApiException.toTimelineRecordDeleteExceptionOrNull(): TimelineRecordDeleteException? {
    val reason =
        when (errorCode) {
            TARGET_UNAVAILABLE_ERROR_CODE -> TimelineRecordDeleteException.Reason.TARGET_UNAVAILABLE
            DATE_OPERATION_ERROR_CODE -> TimelineRecordDeleteException.Reason.DATE_OPERATION_IN_PROGRESS
            PHOTO_DELETE_ERROR_CODE -> TimelineRecordDeleteException.Reason.PHOTO_DELETE_FAILED
            else -> return null
        }
    return TimelineRecordDeleteException(reason, this)
}

private const val TARGET_UNAVAILABLE_ERROR_CODE = -404
private const val DATE_OPERATION_ERROR_CODE = -1016
private const val PHOTO_DELETE_ERROR_CODE = -1017
