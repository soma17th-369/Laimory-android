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
 * - Dialog API(show·resolve·clear)는 메인 스레드 호출을 전제한다 — 요청은 ViewModel(viewModelScope),
 *   응답·정리는 Compose 호스트에서 오므로 호출면이 메인으로 고정되고, 별도 락을 두지 않는다.
 * - 한 번에 하나만 활성화하고, 표시 중 들어온 새 요청은 등록하지 않고 즉시
 *   [DialogResult.Dismissed]로 응답한다(대기열 없음).
 * - 결과는 현재 활성 requestId와 일치할 때만 정확히 한 번 처리한다.
 * - 호출 coroutine이 취소되면 활성 Dialog를 정리한다.
 * - [clearDialogs]는 활성 요청을 결과 전달 없이 취소한다(오래된 결과 실행 방지).
 */
@Singleton
class MessageHelperImpl
    @Inject
    constructor() : MessageHelper {
        private val channel = Channel<UserMessage>(Channel.BUFFERED)
        val messages: Flow<UserMessage> = channel.receiveAsFlow()

        private var lastRequestId = 0L
        private var activeRequest: PendingDialogRequest? = null

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
            val active = activeRequest?.takeIf { it.requestId == requestId } ?: return
            clearActive()
            active.response.complete(result)
        }

        /** 인증 Root 교체 등 전체 정리 시 활성 요청을 결과 전달 없이 종료한다. */
        fun clearDialogs() {
            val active = activeRequest ?: return
            clearActive()
            active.response.cancel()
        }

        private suspend fun awaitDialogResult(request: DialogRequest): DialogResult {
            if (activeRequest != null) return DialogResult.Dismissed
            val pending = PendingDialogRequest(++lastRequestId, CompletableDeferred())
            activeRequest = pending
            _activeDialog.value = ActiveDialog(requestId = pending.requestId, request = request)
            return try {
                pending.response.await()
            } catch (cancellation: CancellationException) {
                // 호출 scope 취소 — 요청자가 죽으면 화면의 Dialog도 함께 정리한다.
                if (activeRequest?.requestId == pending.requestId) clearActive()
                throw cancellation
            }
        }

        private fun clearActive() {
            activeRequest = null
            _activeDialog.value = null
        }

        private class PendingDialogRequest(
            val requestId: Long,
            val response: CompletableDeferred<DialogResult>,
        )
    }
