package com.soma369.laimory.core.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soma369.laimory.core.common.logging.LogDomain
import com.soma369.laimory.core.common.logging.Logger
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI 패턴의 베이스 ViewModel.
 *
 * MVI 흐름: UI → [sendIntent] → [handleIntent] → [updateState] / [sendEffect]
 *
 * Intent는 내부 Channel을 통해 순차적으로 처리되므로,
 * 여러 Intent가 동시에 전달되어도 상태 업데이트와 SideEffect 순서가 보장된다.
 *
 * @param S 화면의 UI 상태. 불변 data class로 구현. [UiState] 참고.
 * @param I 사용자 액션. sealed interface로 구현. [UiIntent] 참고.
 * @param E 일회성 부수 효과. sealed interface로 구현. [UiSideEffect] 참고.
 * @param initialState ViewModel 생성 시 초기 상태.
 */
abstract class BaseMviViewModel<S : UiState, I : UiIntent, E : UiSideEffect>(
    initialState: S,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)

    /** 현재 UI 상태. UI에서 [collectAsStateWithLifecycle]로 구독한다. */
    val state: StateFlow<S> = _state.asStateFlow()

    private val _sideEffect = Channel<E>(Channel.BUFFERED)

    /** 일회성 부수 효과. UI에서 LaunchedEffect 안에서 collect한다. */
    val sideEffect = _sideEffect.receiveAsFlow()

    private val _snackbar = Channel<String>(Channel.BUFFERED)

    /** 스낵바 메시지. UI에서 LaunchedEffect 안에서 collect한다. */
    val snackbar = _snackbar.receiveAsFlow()

    private val intentChannel = Channel<I>(Channel.BUFFERED)

    init {
        viewModelScope.launch {
            for (intent in intentChannel) {
                handleIntent(intent)
            }
        }
    }

    /**
     * UI에서 사용자 액션을 ViewModel로 전달한다.
     *
     * 버퍼가 가득 찬 경우 Intent가 유실되며 로그로 알린다.
     *
     * @param intent 처리할 [UiIntent]
     */
    fun sendIntent(intent: I) {
        val result = intentChannel.trySend(intent)
        if (!result.isSuccess) {
            // 민감정보 방지: intent 객체 전체가 아니라 타입명만 남긴다.
            Logger.w(LogDomain.MVI, "Intent dropped: ${intent::class.simpleName}")
        }
    }

    /**
     * 전달받은 Intent를 처리한다. 서브클래스에서 반드시 구현해야 한다.
     *
     * [updateState] 또는 [sendEffect]를 호출해 상태/효과를 변경한다.
     *
     * @param intent 처리할 [UiIntent]
     */
    protected abstract suspend fun handleIntent(intent: I)

    /**
     * 현재 상태를 변환해 새 상태로 업데이트한다.
     *
     * @param block 현재 상태를 받아 새 상태를 반환하는 람다
     */
    protected fun updateState(block: S.() -> S) {
        _state.update { it.block() }
    }

    /**
     * 일회성 부수 효과를 발행한다.
     *
     * intent 처리 루프를 블로킹하지 않도록 별도 코루틴에서 전송한다.
     *
     * @param effect 발행할 [UiSideEffect]
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _sideEffect.send(effect)
        }
    }

    /**
     * 예외를 안전하게 처리하는 코루틴 래퍼.
     *
     * 블록 내 예외 발생 시 기본적으로 [handleException]을 호출한다.
     * 특수 케이스는 [onError]를 오버라이드해 처리할 수 있다.
     */
    protected fun safeLaunch(
        onError: (Throwable) -> Unit = { handleFailure(it) },
        block: suspend () -> Unit,
    ) = viewModelScope.launch {
        runCatching { block() }.onFailure(onError)
    }

    /**
     * 실패를 화면 피드백(스낵바)으로 표면화한다.
     *
     * 단, [HandledException]은 UseCase에서 이미 공통 정책으로 처리(알림)됐으므로 무시한다.
     * (중복 알림 방지) 서브클래스에서 특수 케이스를 오버라이드할 수 있다.
     */
    protected open fun handleFailure(e: Throwable) {
        if (e is HandledException) return
        val message =
            when (e) {
                is ApiException.NetworkException -> ApiException.NETWORK_ERROR
                is ApiException.UnauthorizedException -> e.message
                is ApiException.ServerException -> ApiException.SERVER_ERROR
                is ApiException.ClientException -> e.message
                is ApiException.ConflictException -> e.message
                is ApiException -> e.message
                else -> ApiException.UNKNOWN_ERROR
            }
        viewModelScope.launch { _snackbar.send(message) }
    }
}
