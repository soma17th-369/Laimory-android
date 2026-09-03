package com.soma369.laimory.core.data.model.push

import kotlinx.serialization.Serializable

/** 전체 푸시와 일일 리마인더의 ON/OFF 요청이 같은 형태라 하나를 함께 쓴다. */
@Serializable
data class PushEnabledRequest(
    val enabled: Boolean,
)
