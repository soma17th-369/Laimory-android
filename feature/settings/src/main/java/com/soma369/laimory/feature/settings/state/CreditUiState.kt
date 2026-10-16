package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState

/**
 * @param remaining 남은 크레딧. 기억해 둔 값이 있으면 들어오자마자 그 값이다. 없거나 못 받았으면 `null`.
 * @param cost 1개당 쓰임(타임라인 한 번 만들기에 드는 크레딧). 서버 값이라 받은 그대로 쓴다.
 * @param isLoading 다시 받는 중. 값이 있으면 받는 중에도 그 값을 보인다 — 로딩은 값이 없고 늦을 때만 띄운다.
 */
@Immutable
data class CreditUiState(
    val remaining: Int? = null,
    val cost: Int? = null,
    val isLoading: Boolean = true,
) : UiState
