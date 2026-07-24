package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.exception.TimelineRecordDeleteException

internal sealed interface TimelineDeleteFailureAction {
    data object TargetUnavailable : TimelineDeleteFailureAction

    data object RecordAlreadySaved : TimelineDeleteFailureAction

    data object AlreadyHandled : TimelineDeleteFailureAction

    data class Retryable(
        val message: String,
    ) : TimelineDeleteFailureAction
}

internal fun Throwable.toTimelineDeleteFailureAction(): TimelineDeleteFailureAction =
    when (this) {
        is TimelineRecordDeleteException ->
            when (reason) {
                TimelineRecordDeleteException.Reason.TARGET_UNAVAILABLE ->
                    TimelineDeleteFailureAction.TargetUnavailable
                TimelineRecordDeleteException.Reason.RECORD_ALREADY_SAVED ->
                    TimelineDeleteFailureAction.RecordAlreadySaved
                TimelineRecordDeleteException.Reason.DATE_OPERATION_IN_PROGRESS ->
                    TimelineDeleteFailureAction.Retryable(DATE_OPERATION_MESSAGE)
                TimelineRecordDeleteException.Reason.PHOTO_DELETE_FAILED ->
                    TimelineDeleteFailureAction.Retryable(PHOTO_DELETE_MESSAGE)
            }
        is HandledException -> TimelineDeleteFailureAction.AlreadyHandled
        is ApiException.NetworkException -> TimelineDeleteFailureAction.Retryable(NETWORK_MESSAGE)
        else -> TimelineDeleteFailureAction.Retryable(GENERIC_MESSAGE)
    }

private const val DATE_OPERATION_MESSAGE = "같은 날짜의 작업이 진행 중이에요. 잠시 후 다시 시도해주세요."
private const val PHOTO_DELETE_MESSAGE = "서버 사진을 삭제하지 못했어요. 잠시 후 다시 시도해주세요."
private const val NETWORK_MESSAGE = "네트워크 상태를 확인한 뒤 다시 시도해주세요."
private const val GENERIC_MESSAGE = "일시적인 오류예요. 잠시 후 다시 시도해주세요."
