package com.soma369.laimory.core.ui.base

/**
 * MVI 패턴의 사용자 의도(이벤트)를 나타내는 마커 인터페이스.
 *
 * UI에서 발생하는 모든 사용자 액션은 sealed interface로 구현하고,
 * [BaseMviViewModel.sendIntent]를 통해 ViewModel로 전달한다.
 *
 * 예시:
 * ```kotlin
 * sealed interface HomeUiIntent : UiIntent {
 *     data object Increment : HomeUiIntent
 *     data object Decrement : HomeUiIntent
 * }
 * ```
 */
interface UiIntent
