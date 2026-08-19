package com.soma369.laimory.core.domain.usecase.user

import com.soma369.laimory.core.domain.coordinator.UserProfileCoordinator
import javax.inject.Inject

/**
 * 아직 회원 정보를 받지 못했으면 다시 조회하게 한다.
 *
 * 조회는 실패해도 화면을 막지 않으므로, 화면에 다시 들어왔을 때 이 호출로 만회할 기회를 준다.
 * 이미 성공한 세션에서는 아무 일도 하지 않는다.
 */
class RefreshUserProfileUseCase
    @Inject
    constructor(
        private val coordinator: UserProfileCoordinator,
    ) {
        operator fun invoke() = coordinator.refresh()
    }
