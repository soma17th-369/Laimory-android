package com.soma369.laimory.core.data.model.feature1

import com.soma369.laimory.core.domain.model.Feature1Item
import kotlinx.serialization.Serializable

@Serializable
data class Feature1ItemResponse(
    val id: Int?,
    val title: String?,
    val description: String?,
)

internal fun Feature1ItemResponse.toDomain() =
    Feature1Item(
        id = id ?: 0,
        title = title.orEmpty(),
        description = description.orEmpty(),
    )
