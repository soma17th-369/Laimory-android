package com.soma369.laimory.core.data.model.credit

import com.soma369.laimory.core.domain.model.credit.CreditCosts
import kotlinx.serialization.Serializable

/**
 * `GET /credit/costs` 응답. 크레딧을 쓰는 기능이 늘면 서버가 같은 응답에 필드를 더한다고 예고했다 — 모르는 필드는
 * 공용 Json 설정(`ignoreUnknownKeys`)으로 넘긴다.
 */
@Serializable
data class CreditCostsResponse(
    val timelineCreation: Int,
)

internal fun CreditCostsResponse.toDomain(): CreditCosts = CreditCosts(timelineCreation = timelineCreation)
