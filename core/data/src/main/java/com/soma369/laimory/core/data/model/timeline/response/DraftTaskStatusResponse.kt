package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.DraftTaskFailureReason
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatus
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
    val error: Int? = null,
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
            if (result != null || error == null || elapsedSeconds != null) invalidShape()
            DraftTaskSnapshot(
                status = taskStatus,
                failure = error.toFailureReason(),
            )
        }
    }
}

private fun DraftTaskStatusResponse.invalidShape(): Nothing =
    throw ApiException.UnknownException("초안 작업 응답 필드가 status=$status 계약과 일치하지 않습니다")

private fun Int.toFailureReason(): DraftTaskFailureReason =
    when (this) {
        -1008 -> DraftTaskFailureReason.AI_REPORTED_FAILURE
        -1009 -> DraftTaskFailureReason.AI_DISPATCH_FAILURE
        -1010 -> DraftTaskFailureReason.STAGING_DATA_MISSING
        -1011 -> DraftTaskFailureReason.FINALIZE_FAILURE
        else -> DraftTaskFailureReason.UNKNOWN
    }
