package com.soma369.laimory.core.data.model.user

import kotlinx.serialization.Serializable

/**
 * `GET users/me` 응답 body.
 *
 * 서버는 key 를 생략하지 않고 값이 없으면 명시적 JSON null 을 보낸다. 닉네임을 정하지 않은 계정이
 * 정상 상태이므로 null 은 오류가 아니다.
 */
@Serializable
data class UserProfileResponse(
    val nickname: String? = null,
)
