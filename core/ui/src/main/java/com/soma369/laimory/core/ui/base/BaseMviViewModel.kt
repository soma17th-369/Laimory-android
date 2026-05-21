package com.soma369.laimory.core.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val intentChannel = Channel<I>(Channel.UNLIMITED)

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
     * @param intent 처리할 [UiIntent]
     */
    fun sendIntent(intent: I) {
        intentChannel.trySend(intent)
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
     * @param effect 발행할 [UiSideEffect]
     */
    protected suspend fun sendEffect(effect: E) {
        _sideEffect.send(effect)
    }
}
