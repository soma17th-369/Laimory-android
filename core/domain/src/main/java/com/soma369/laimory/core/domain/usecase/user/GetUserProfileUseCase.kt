package com.soma369.laimory.core.domain.usecase.user

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.repository.UserProfileRepository
import javax.inject.Inject

/**
 * 현재 인증 세션 계정의 회원 정보를 조회한다.
 *
 * 공통 [com.soma369.laimory.core.domain.base.BaseUseCase] 를 쓰지 않는다. 그쪽은 404·5xx 에도
 * 사용자 메시지를 발행하는데, 이 조회는 사용자가 요청한 적 없는 배경 개인화라 실패했다고 홈 진입마다
 * 오류 안내가 뜨면 안 된다. 닉네임이 없으면 화면이 fallback 문구를 쓰면 그만이다.
 *
 * 401 만 예외다. 실제 세션 만료이므로 공통 정책에 그대로 넘겨 재로그인을 유도한다.
 */
class GetUserProfileUseCase
    @Inject
    constructor(
        private val repository: UserProfileRepository,
        private val messageHelper: MessageHelper,
    ) {
        suspend operator fun invoke(): Result<UserProfile> =
            try {
                Result.success(repository.getMyProfile())
            } catch (e: ApiException) {
                Result.failure(handlePolicy(e))
            }

        private fun handlePolicy(e: ApiException): Throwable {
            if (e.rawCode != UNAUTHORIZED_CODE) return e
            messageHelper.send(UserMessage.SessionExpired)
            return HandledException(e)
        }

        private companion object {
            const val UNAUTHORIZED_CODE = 401
        }
    }
