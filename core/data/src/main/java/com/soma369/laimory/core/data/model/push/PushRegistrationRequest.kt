package com.soma369.laimory.core.data.model.push

import kotlinx.serialization.Serializable

/** FID는 민감한 opaque 식별자이므로 문자열 표현에 원문을 포함하지 않는다. */
@Serializable
data class PushRegistrationRequest(
    val firebaseInstallationId: String,
) {
    override fun toString(): String = "PushRegistrationRequest(REDACTED)"
}
