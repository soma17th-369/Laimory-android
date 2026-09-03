package com.soma369.laimory.core.domain.usecase.push

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.repository.PushSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 전체 푸시 수신을 켜고 끈다.
 *
 * 꺼도 FCM 등록을 해제하지 않는다 — 등록은 로그인·로그아웃 경계가 관리하고, 여기서는 서버가
 * 발송만 막는다. 해제까지 하면 다시 켤 때 등록이 살아 있으리라는 보장이 없다.
 */
@Singleton
class UpdatePushEnabledUseCase
    @Inject
    constructor(
        private val repository: PushSettingsRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(isEnabled: Boolean): Result<Unit> = execute { repository.updatePushEnabled(isEnabled) }
    }
