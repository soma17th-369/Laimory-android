package com.soma369.laimory.core.data.model.onboarding

import kotlinx.serialization.Serializable

/** `GET /initializer` 응답. 앱 시작에 필요한 계정 단위 설정이다. */
@Serializable
data class AppInitializerResponse(
    val onboardingCompleted: Boolean,
)
