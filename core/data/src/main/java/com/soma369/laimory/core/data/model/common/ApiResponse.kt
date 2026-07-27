package com.soma369.laimory.core.data.model.common

import kotlinx.serialization.Serializable

/**
 * 서버 공통 응답 래퍼. 모든 응답은 [header](메타) + [body](payload) 구조로 온다.
 */
@Serializable
data class ApiResponse<T>(
    val header: ApiHeader,
    val body: T?,
)

@Serializable
data class ApiHeader(
    val code: Int,
    val message: String? = null,
)

/** 서버 공통 성공 코드. `header.code`가 이 값이면 성공으로 간주한다. */
const val SUCCESS_CODE: Int = 0
