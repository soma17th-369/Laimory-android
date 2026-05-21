package com.soma369.laimory.core.ui.base

/**
 * MVI 패턴의 UI 상태를 나타내는 마커 인터페이스.
 *
 * 모든 UiState는 불변(immutable) data class로 구현해야 하며,
 * 상태 변경은 [BaseMviViewModel.updateState]를 통해서만 이루어진다.
 * Compose 리컴포지션 최적화를 위해 구현체 data class에 @Immutable을 붙인다.
 *
 * 예시:
 * ```kotlin
 * @Immutable
 * data class HomeUiState(
 *     val counter: Int = 0,
 *     val isLoading: Boolean = false,
 * ) : UiState
 * ```
 */
interface UiState
