package com.soma369.laimory.core.data.model.timeline.response

import kotlinx.serialization.Serializable

/** `POST /timeline/drafts/check` 응답. */
@Serializable
data class DraftCreatableCheckResponse(
    val creatable: Boolean,
)
