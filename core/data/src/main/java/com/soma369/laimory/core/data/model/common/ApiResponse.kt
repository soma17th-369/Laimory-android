package com.soma369.laimory.core.data.model.common

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val error: ApiError?,
)
