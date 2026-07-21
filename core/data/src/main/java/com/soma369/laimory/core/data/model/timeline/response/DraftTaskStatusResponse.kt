package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatus
import com.soma369.laimory.core.domain.model.timeline.TimelineErrorCode
import kotlinx.serialization.Serializable

/**
 * `GET /timeline/drafts/{taskId}` 응답.
 *
 * PROCESSING은 [elapsedSeconds], SUCCESS는 [result], FAILED는 [error]를 사용한다.
 */
@Serializable
data class DraftTaskStatusResponse(
    val status: String,
    val result: DailyTimelineResponse? = null,
    val error: String? = null,
    val elapsedSeconds: Long? = null,
)

internal fun DraftTaskStatusResponse.toDomain(): DraftTaskSnapshot {
    val taskStatus =
        DraftTaskStatus.entries.firstOrNull { it.name == status }
            ?: throw ApiException.UnknownException("알 수 없는 초안 작업 상태입니다: $status")

    return when (taskStatus) {
        DraftTaskStatus.PROCESSING -> {
            if (result != null || error != null || elapsedSeconds?.let { it < 0 } == true) invalidShape()
            DraftTaskSnapshot(status = taskStatus, elapsedSeconds = elapsedSeconds)
        }

        DraftTaskStatus.SUCCESS -> {
            if (result == null || error != null || elapsedSeconds != null) invalidShape()
            DraftTaskSnapshot(status = taskStatus, result = result.toDomain())
        }

        DraftTaskStatus.FAILED -> {
            if (result != null || error.isNullOrBlank() || elapsedSeconds != null) invalidShape()
            DraftTaskSnapshot(
                status = taskStatus,
                failure = TimelineErrorCode.fromServerValue(error),
            )
        }
    }
}

private fun DraftTaskStatusResponse.invalidShape(): Nothing =
    throw ApiException.UnknownException("초안 작업 응답 필드가 status=$status 계약과 일치하지 않습니다")
