package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import kotlinx.serialization.Serializable

/** `POST /timeline/drafts` 응답(202). 비동기 작업 식별자만 온다. */
@Serializable
data class CreateDraftTaskResponse(
    val taskId: String,
)

internal fun CreateDraftTaskResponse.toDomain() = DraftTaskHandle(taskId = taskId)
