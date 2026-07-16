package com.soma369.laimory.core.domain.helper

import com.soma369.laimory.core.domain.model.auth.SocialLoginCallback
import kotlinx.coroutines.flow.Flow

/** Activity가 수신한 로그인 callback을 로그인 화면에 한 번 전달하는 프로세스 내 브릿지. */
interface SocialLoginCallbackHandler {
    val callbacks: Flow<SocialLoginCallback>

    fun handle(callback: SocialLoginCallback)
}
