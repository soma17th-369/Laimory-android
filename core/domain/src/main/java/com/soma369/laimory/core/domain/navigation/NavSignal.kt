package com.soma369.laimory.core.domain.navigation

/**
 * 단일 내비게이션 플로우에 흘려보내는 신호. 전진/후진 모두 이 한 형식으로 emit 된다.
 *
 * - [GoToDestPage]: 앱 내 전진 이동. 현재 백스택 위로 push 한다.
 * - [ReplaceRoot]: 현재 이력을 제거하고 새 루트로 교체한다.
 * - [Back]: 시스템 백 키와 동일하게 한 단계 뒤로.
 *
 * 딥링크(웜 스타트 bring-to-front)는 첫 딥링크가 정의될 때 `DeepLink` 로 추가한다.
 */
sealed interface NavSignal {
    data class GoToDestPage(val route: NavRoute) : NavSignal

    data class ReplaceRoot(val route: NavRoute) : NavSignal

    data object Back : NavSignal
}
