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

abstract class MviViewModel<S : UiState, I : UiIntent, E : UiSideEffect>(
    initialState: S,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _sideEffect = Channel<E>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    fun sendIntent(intent: I) {
        viewModelScope.launch { handleIntent(intent) }
    }

    protected abstract suspend fun handleIntent(intent: I)

    protected fun updateState(block: S.() -> S) {
        _state.update { it.block() }
    }

    protected suspend fun sendEffect(effect: E) {
        _sideEffect.send(effect)
    }
}
