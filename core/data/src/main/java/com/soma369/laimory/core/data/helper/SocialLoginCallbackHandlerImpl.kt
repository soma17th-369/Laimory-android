package com.soma369.laimory.core.data.helper

import com.soma369.laimory.core.domain.helper.SocialLoginCallbackHandler
import com.soma369.laimory.core.domain.model.auth.SocialLoginCallback
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialLoginCallbackHandlerImpl
    @Inject
    constructor() : SocialLoginCallbackHandler {
        private val channel = Channel<SocialLoginCallback>(Channel.BUFFERED)
        override val callbacks: Flow<SocialLoginCallback> = channel.receiveAsFlow()

        override fun handle(callback: SocialLoginCallback) {
            channel.trySend(callback)
        }
    }
