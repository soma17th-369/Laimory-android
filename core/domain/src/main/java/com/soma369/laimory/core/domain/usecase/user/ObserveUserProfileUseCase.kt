package com.soma369.laimory.core.domain.usecase.user

import com.soma369.laimory.core.domain.coordinator.UserProfileCoordinator
import com.soma369.laimory.core.domain.model.user.UserProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 화면이 공용 회원 정보를 구독한다. 화면마다 서버를 직접 부르지 않게 하는 진입점이다. */
class ObserveUserProfileUseCase
    @Inject
    constructor(
        private val coordinator: UserProfileCoordinator,
    ) {
        operator fun invoke(): Flow<UserProfile?> = coordinator.profile
    }
