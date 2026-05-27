package com.soma369.laimory.core.data.dto.common

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val code: String,
    val message: String,
)
