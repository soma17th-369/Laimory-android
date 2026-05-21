package com.soma369.laimory.core.ui.base

/**
 * MVI 패턴의 일회성 부수 효과를 나타내는 마커 인터페이스.
 *
 * 상태(State)로 표현하기 어려운 일회성 이벤트(Toast, Navigation 등)에 사용한다.
 * [BaseMviViewModel.sendEffect]로 발행되며, UI에서는 LaunchedEffect로 수집한다.
 *
 * 예시:
 * ```kotlin
 * sealed interface HomeUiSideEffect : UiSideEffect {
 *     data class ShowToast(val message: String) : HomeUiSideEffect
 * }
 * ```
 */
interface UiSideEffect
