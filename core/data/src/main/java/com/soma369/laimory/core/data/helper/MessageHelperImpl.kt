package com.soma369.laimory.core.data.helper

import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.ActiveDialog
import com.soma369.laimory.core.domain.message.DialogRequest
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.message.UserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MessageHelper]의 구현체 겸 브릿지.
 *
 * 도메인(SingletonComponent)에서 발행한 [UserMessage]를 Channel로 받아,
 * Compose 호스트(app `LaimoryNavGraph`)가 [messages]를 수집해 실제 UI(스낵바 등)로 매핑한다.
 * (@Singleton impl은 Compose 상태를 직접 못 가지므로 브릿지)
 *
 * 공통 Dialog는 다음 정책으로 관리한다.
 * - 한 번에 하나만 활성화하고 나머지는 FIFO로 대기한다.
 * - 같은 key의 활성·대기 요청은 등록하지 않고 즉시 [DialogResult.Dismissed]로 응답한다.
 * - 결과는 현재 활성 requestId와 일치할 때만 정확히 한 번 처리한다.
 * - 호출 coroutine이 취소되면 해당 활성·대기 요청을 제거한다.
 * - [clearDialogs]는 활성·대기 요청을 결과 전달 없이 취소한다(오래된 결과 실행 방지).
 */
@Singleton
class MessageHelperImpl
    @Inject
    constructor() : MessageHelper {
        private val channel = Channel<UserMessage>(Channel.BUFFERED)
        val messages: Flow<UserMessage> = channel.receiveAsFlow()

        private val lock = Any()
        private var lastRequestId = 0L
        private var activeRequest: PendingDialogRequest? = null
        private val pendingQueue = ArrayDeque<PendingDialogRequest>()

        private val _activeDialog = MutableStateFlow<ActiveDialog?>(null)

        /** Root DialogHost가 렌더링하는 현재 활성 Dialog. 구성 변경 후 재수집돼도 응답 전까지 유지된다. */
        val activeDialog: StateFlow<ActiveDialog?> = _activeDialog.asStateFlow()

        override fun send(message: UserMessage) {
            channel.trySend(message)
        }

        override suspend fun showOneButtonDialog(request: DialogRequest.OneButton): DialogResult = awaitDialogResult(request)

        override suspend fun showTwoButtonDialog(request: DialogRequest.TwoButton): DialogResult = awaitDialogResult(request)

        /** Root 호스트가 사용자 선택을 반환한다. 활성 [requestId]와 일치하지 않으면 무시한다. */
        fun resolveDialog(
            requestId: Long,
            result: DialogResult,
        ) {
            val resolved =
                synchronized(lock) {
                    val active = activeRequest?.takeIf { it.requestId == requestId } ?: return
                    activeRequest = null
                    promoteNextLocked()
                    active
                }
            resolved.response.complete(result)
        }

        /** 인증 Root 교체 등 전체 정리 시 활성 요청과 대기열을 결과 전달 없이 종료한다. */
        fun clearDialogs() {
            val cleared =
                synchronized(lock) {
                    val all =
                        buildList {
                            activeRequest?.let(::add)
                            addAll(pendingQueue)
                        }
                    activeRequest = null
                    pendingQueue.clear()
                    _activeDialog.value = null
                    all
                }
            cleared.forEach { it.response.cancel() }
        }

        private suspend fun awaitDialogResult(request: DialogRequest): DialogResult {
            val pending =
                synchronized(lock) {
                    val isDuplicateKey =
                        activeRequest?.request?.key == request.key ||
                            pendingQueue.any { it.request.key == request.key }
                    if (isDuplicateKey) return DialogResult.Dismissed
                    PendingDialogRequest(++lastRequestId, request, CompletableDeferred()).also { created ->
                        if (activeRequest == null) activateLocked(created) else pendingQueue.addLast(created)
                    }
                }
            return try {
                pending.response.await()
            } catch (cancellation: CancellationException) {
                // 호출 scope 취소(또는 clearDialogs) — 오래된 요청이 화면·대기열에 남지 않게 한다.
                removeRequest(pending.requestId)
                throw cancellation
            }
        }

        private fun removeRequest(requestId: Long) {
            synchronized(lock) {
                if (activeRequest?.requestId == requestId) {
                    activeRequest = null
                    promoteNextLocked()
                } else {
                    pendingQueue.removeAll { it.requestId == requestId }
                }
            }
        }

        private fun activateLocked(request: PendingDialogRequest) {
            activeRequest = request
            _activeDialog.value = ActiveDialog(requestId = request.requestId, request = request.request)
        }

        private fun promoteNextLocked() {
            val next = pendingQueue.removeFirstOrNull()
            if (next == null) {
                _activeDialog.value = null
            } else {
                activateLocked(next)
            }
        }

        private class PendingDialogRequest(
            val requestId: Long,
            val request: DialogRequest,
            val response: CompletableDeferred<DialogResult>,
        )
    }
