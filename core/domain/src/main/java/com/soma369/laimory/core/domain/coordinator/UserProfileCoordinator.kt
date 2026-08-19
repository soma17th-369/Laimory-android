package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.model.user.UserProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * 인증 세션 하나당 회원 정보를 한 번만 조회해 화면들이 나눠 쓰게 한다.
 *
 * 홈과 설정이 각자 조회하면 앱 시작마다 같은 요청이 두 번 나가고, 계정이 바뀌었을 때 비우는
 * 규칙도 화면마다 흩어진다. 조회 시점과 무효화를 여기 한 곳에 모은다.
 */
interface UserProfileCoordinator {
    /**
     * 현재 세션에서 조회된 회원 정보. 아직 조회 전이거나 로그아웃했으면 `null` 이다.
     *
     * 닉네임이 없는 계정은 `null` 이 아니라 `nickname` 만 비어 있는 [UserProfile] 로 온다 —
     * "아직 모른다" 와 "없는 게 확실하다" 를 화면이 구분할 수 있어야 한다.
     */
    val profile: StateFlow<UserProfile?>

    /**
     * 아직 조회하지 못했으면 다시 시도한다.
     *
     * 이미 성공한 세션이면 아무것도 하지 않고, 진행 중인 요청이 있으면 거기에 합류한다.
     * 실패 후 화면에 다시 들어오거나 전경으로 돌아왔을 때 호출한다.
     */
    fun refresh()
}
