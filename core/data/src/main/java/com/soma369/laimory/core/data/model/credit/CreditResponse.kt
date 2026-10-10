package com.soma369.laimory.core.data.model.credit

import kotlinx.serialization.Serializable

/** `GET /credit` 응답. 총량 필드는 없다 — 가입 때 받은 양에서 얼마나 썼는지는 서버가 주지 않는다. */
@Serializable
data class CreditResponse(
    val remainingCredits: Int,
)
