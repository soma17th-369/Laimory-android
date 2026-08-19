package com.soma369.laimory.core.data.model.user

import com.soma369.laimory.core.domain.model.user.UserProfile

/**
 * 서버 응답을 도메인 [UserProfile] 로 옮긴다.
 *
 * 공백만 있는 닉네임은 화면에 `안녕하세요, 님` 처럼 새므로 표시할 값이 없는 것으로 본다.
 * 정규화는 [UserProfile] 이 갖고 여기서는 전달만 한다.
 */
internal fun UserProfileResponse.toDomain(): UserProfile = UserProfile.of(nickname)
