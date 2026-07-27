package com.soma369.laimory.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LaimoryFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var handler: DraftCompletionPushHandler

    override fun onRegistered(installationId: String) {
        Logger.i(
            LogDomain.PUSH,
            "FCM 등록 콜백 수신(fid=${installationId.maskedId()})",
        )
        handler.onRegistered(installationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Logger.i(
            LogDomain.PUSH,
            "FCM 메시지 수신(" +
                "messageId=${message.messageId ?: "none"}, " +
                "notification=${message.notification != null}, " +
                "dataKeys=${message.data.keys.sorted()}" +
                ")",
        )
        handler.onMessage(message.data)
    }

    override fun onDeletedMessages() {
        Logger.w(LogDomain.PUSH, "FCM 대기 메시지가 삭제됨 — 서버 상태 재조회 필요")
    }

    private fun String.maskedId(): String =
        when {
            isBlank() -> "blank"
            length <= MASKED_SUFFIX_LENGTH -> "***"
            else -> "***${takeLast(MASKED_SUFFIX_LENGTH)}"
        }

    private companion object {
        const val MASKED_SUFFIX_LENGTH = 6
    }
}
