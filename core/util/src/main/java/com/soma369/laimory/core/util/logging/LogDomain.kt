package com.soma369.laimory.core.util.logging

/**
 * 로그 영역 분류 태그. [Logger]의 domain 파라미터(= Logcat tag)로 사용한다.
 *
 * **UseCase 영역은 두지 않는다.** 도메인 계층(`core:domain`)은 순수 JVM 모듈이라 `android.util.Log`
 * 에 기대는 이 로거를 쓸 수 없다. 상수만 있으면 쓸 수 없는 자리가 쓸 수 있는 것처럼 보인다.
 * UseCase 의 결과는 그것을 부른 쪽이 남기고, 도메인 로그가 실제로 필요해지면 로거를 순수 JVM
 * 모듈로 옮기고 출력만 포트로 뺀다.
 */
object LogDomain {
    const val MVI = "Mvi"
    const val NAVIGATION = "Navigation"
    const val NETWORK = "Network"
    const val REPOSITORY = "Repository"
    const val COLLECTION = "Collection"
    const val DRAFT_TASK = "DraftTask"
    const val PUSH = "Push"

    /** 소셜 로그인처럼 앱 밖을 거쳐 돌아오는 인증 흐름. 시작과 끝을 짝지어 남긴다. */
    const val AUTH = "Auth"

    /** 사용자가 확정한, 서버나 기기 상태를 바꾸는 행동. 화면 이동은 넣지 않는다. */
    const val USER_ACTION = "UserAction"
}
