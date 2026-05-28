package com.soma369.laimory.core.data.model.common

data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?,
    val error: ApiError?,
)
