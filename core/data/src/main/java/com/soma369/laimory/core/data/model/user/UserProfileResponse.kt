package com.soma369.laimory.core.data.model.user

import kotlinx.serialization.Serializable

/**
 * `GET user` 응답 body.
 *
 * 서버는 key 를 생략하지 않고 값이 없으면 명시적 JSON null 을 보낸다. 닉네임을 정하지 않은 계정이
 * 정상 상태이므로 null 은 오류가 아니다.
 *
 * [userId] 는 서버 계약상 항상 있지만(회원 행 PK, access token `sub` 와 같은 값) 없어도 받는다.
 * 필수로 두면 이 필드를 아직 내려주지 않는 서버에서 응답 전체가 깨져 닉네임까지 사라진다.
 */
@Serializable
data class UserProfileResponse(
    val userId: Long? = null,
    val nickname: String? = null,
)
