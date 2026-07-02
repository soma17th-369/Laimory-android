package com.soma369.laimory.core.data.model.common

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val code: String?,
    val detail: String?,
)
