package com.soma369.laimory.push

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.domain.usecase.push.RegisterPushInstallationUseCase
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DraftCompletionPushHandler
    @Inject
    constructor(
        private val registerPushInstallation: RegisterPushInstallationUseCase,
        private val observeAuthSession: ObserveAuthSessionUseCase,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) {
        fun onRegistered(firebaseInstallationId: String) {
            if (firebaseInstallationId.isBlank()) {
                Logger.w(LogDomain.PUSH, "빈 FID가 전달되어 서버 등록을 건너뜀")
                return
            }
            applicationScope.launch {
                val session =
                    observeAuthSession().first { state ->
                        state != AuthSessionState.Loading
                    }
                if (session == AuthSessionState.Authenticated) {
                    registerPushInstallation(firebaseInstallationId)
                        .onSuccess {
                            Logger.i(LogDomain.PUSH, "FID 서버 등록 성공")
                        }.onFailure { error ->
                            Logger.e(LogDomain.PUSH, "FID 서버 등록 실패", error)
                        }
                } else {
                    Logger.w(LogDomain.PUSH, "미인증 상태라 FID 서버 등록을 건너뜀")
                }
            }
        }

        fun onMessage(data: Map<String, String>) {
            val signal =
                DraftCompletionSignalParser.parse(data)
                    ?: run {
                        Logger.w(
                            LogDomain.PUSH,
                            "FCM data 파싱 실패(dataKeys=${data.keys.sorted()})",
                        )
                        return
                    }
            Logger.i(
                LogDomain.PUSH,
                "초안 완료 신호 처리(taskId=${signal.taskId.maskedId()}, status=${signal.status})",
            )
            draftTaskCoordinator.refreshFromCompletionSignal(signal.taskId)
        }

        suspend fun onNotificationOpened(
            taskId: String?,
            status: String?,
        ): Boolean {
            val signal =
                DraftCompletionSignalParser.parse(taskId, status)
                    ?: run {
                        Logger.w(
                            LogDomain.PUSH,
                            "알림 탭 data 파싱 실패(" +
                                "taskIdPresent=${!taskId.isNullOrBlank()}, " +
                                "status=$status" +
                                ")",
                        )
                        return false
                    }
            val session =
                observeAuthSession().first { state ->
                    state != AuthSessionState.Loading
                }
            if (session != AuthSessionState.Authenticated) {
                Logger.w(LogDomain.PUSH, "미인증 상태라 초안 완료 알림 탭 처리를 건너뜀")
                return false
            }
            Logger.i(
                LogDomain.PUSH,
                "초안 완료 알림 탭 처리(taskId=${signal.taskId.maskedId()}, status=${signal.status})",
            )
            draftTaskCoordinator.refreshFromCompletionSignal(signal.taskId)
            return true
        }

        private fun String.maskedId(): String =
            when {
                length <= MASKED_SUFFIX_LENGTH -> "***"
                else -> "***${takeLast(MASKED_SUFFIX_LENGTH)}"
            }

        private companion object {
            const val MASKED_SUFFIX_LENGTH = 6
        }
    }
