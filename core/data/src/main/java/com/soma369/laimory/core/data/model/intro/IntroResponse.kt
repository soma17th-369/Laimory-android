package com.soma369.laimory.core.data.model.intro

import com.soma369.laimory.core.domain.model.IntroInfo
import kotlinx.serialization.Serializable

@Serializable
data class IntroResponse(
    val minAppVersion: Int?,
    val recommendAppVersion: Int?,
    val debugTestMessage: String?,
)

internal fun IntroResponse.toDomain() =
    IntroInfo(
        minAppVersion = minAppVersion ?: 0,
        recommendAppVersion = recommendAppVersion ?: 0,
        debugTestMessage = debugTestMessage.orEmpty(),
    )
