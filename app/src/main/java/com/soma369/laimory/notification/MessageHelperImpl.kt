package com.soma369.laimory.notification

import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MessageHelper]의 앱 구현체 겸 브릿지.
 *
 * 도메인(SingletonComponent)에서 발행한 [UserMessage]를 Channel로 받아,
 * Compose 호스트([com.soma369.laimory.navigation.LaimoryNavGraph])가 [messages]를
 * 수집해 실제 UI(스낵바 등)로 매핑한다. (@Singleton impl은 Compose 상태를 직접 못 가지므로 브릿지)
 */
@Singleton
class MessageHelperImpl
    @Inject
    constructor() : MessageHelper {
        private val channel = Channel<UserMessage>(Channel.BUFFERED)
        val messages: Flow<UserMessage> = channel.receiveAsFlow()

        override fun send(message: UserMessage) {
            channel.trySend(message)
        }
    }
