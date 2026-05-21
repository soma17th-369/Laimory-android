package com.soma369.laimory.core.ui.base

import androidx.compose.runtime.Immutable

/**
 * MVI 패턴의 UI 상태를 나타내는 마커 인터페이스.
 *
 * [@Immutable]을 통해 Compose 컴파일러에 불변성을 보장하며, 불필요한 리컴포지션을 방지한다.
 * 모든 UiState는 불변(immutable) data class로 구현해야 하며,
 * 상태 변경은 [BaseMviViewModel.updateState]를 통해서만 이루어진다.
 *
 * 예시:
 * ```kotlin
 * data class HomeUiState(
 *     val counter: Int = 0,
 *     val isLoading: Boolean = false,
 * ) : UiState
 * ```
 */
@Immutable
interface UiState
