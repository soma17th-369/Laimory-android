package com.soma369.laimory.notification

import com.soma369.laimory.core.domain.notification.UserNotification
import com.soma369.laimory.core.domain.notification.UserNotifier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UserNotifier]의 앱 구현체 겸 브릿지.
 *
 * 도메인(SingletonComponent)에서 발행한 [UserNotification]을 Channel로 받아,
 * Compose 호스트([com.soma369.laimory.navigation.LaimoryNavGraph])가 [notifications]를
 * 수집해 실제 UI(스낵바 등)로 매핑한다. (@Singleton impl은 Compose 상태를 직접 못 가지므로 브릿지)
 */
@Singleton
class UserNotifierImpl
    @Inject
    constructor() : UserNotifier {
        private val channel = Channel<UserNotification>(Channel.BUFFERED)
        val notifications: Flow<UserNotification> = channel.receiveAsFlow()

        override fun notify(notification: UserNotification) {
            channel.trySend(notification)
        }
    }
