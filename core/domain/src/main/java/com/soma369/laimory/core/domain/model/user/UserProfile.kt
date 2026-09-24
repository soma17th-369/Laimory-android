package com.soma369.laimory.core.domain.model.user

/**
 * 현재 인증 세션 계정의 회원 정보.
 *
 * [nickname] 은 표시 가능한 값만 담는다 — 서버가 null 을 주거나 공백뿐이면 null 로 정규화한다.
 * 닉네임을 정하지 않은 계정은 정상 상태이므로 화면은 이 값이 없을 때의 문구를 항상 갖춰야 한다.
 *
 * [userId] 는 회원 행 PK 다. 화면에 보이지 않고 제품 분석의 사용자 구분에만 쓴다. 이 값을 내려주지
 * 않는 서버에서는 null 이다.
 */
data class UserProfile(
    val nickname: String?,
    val userId: Long? = null,
) {
    companion object {
        /** 서버가 준 원본 닉네임을 표시 가능한 값으로 정규화해 만든다. */
        fun of(
            rawNickname: String?,
            userId: Long? = null,
        ): UserProfile =
            UserProfile(
                nickname = rawNickname?.trim()?.takeIf(String::isNotEmpty),
                userId = userId,
            )
    }
}
