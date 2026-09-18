package com.soma369.laimory

import androidx.browser.auth.AuthTabIntent

/** Auth Tab 이 끝났을 때 할 일. */
internal sealed interface AuthTabOutcome {
    /** 콜백 주소로 끝났다. 로그인 화면에 넘긴다. */
    data class Deliver(
        val callbackUri: String,
    ) : AuthTabOutcome

    /** 콜백 주소 소유 확인에 실패했다. 같은 인증 페이지를 일반 Custom Tab 으로 다시 연다. */
    data class Reopen(
        val authorizationUrl: String,
        val reason: String,
    ) : AuthTabOutcome

    /**
     * 콜백 없이 닫혔다. 사용자가 닫았거나, Auth Tab 을 모르는 브라우저로 열려 App Link 가 탭을 정리한
     * 경우다 — 앞은 로그인 화면이 복귀를 보고 취소를 판정하고, 뒤는 `onNewIntent` 가 콜백을 받는다.
     */
    data object Closed : AuthTabOutcome

    /** 콜백 없이 끝났고 다시 열 수도 없다. */
    data class Failed(
        val reason: String,
    ) : AuthTabOutcome
}

/**
 * Auth Tab 결과를 할 일로 바꾼다.
 *
 * 소유 확인 실패만 다시 연다. 이 확인은 Auth Tab 에만 있는 단계라, 여기서 막히면 기존 Custom Tab 으로
 * 열어야 이 변경 전과 같은 결과가 난다. 서버 assetlinks 에 서명이 빠진 경우라면 App Link 도 같이 깨져
 * 다시 열어도 돌아오지 못하지만, 그래도 이 변경 전보다 나빠지지는 않는다.
 *
 * @param authorizationUrl 이번에 연 인증 주소. 프로세스가 재시작돼 잃었으면 null 이고, 다시 열 수 없다.
 */
internal fun authTabOutcome(
    resultCode: Int,
    resultUri: String?,
    authorizationUrl: String?,
): AuthTabOutcome =
    when (resultCode) {
        AuthTabIntent.RESULT_OK ->
            resultUri?.let(AuthTabOutcome::Deliver) ?: AuthTabOutcome.Failed("성공 결과에 주소가 없다")
        AuthTabIntent.RESULT_CANCELED -> AuthTabOutcome.Closed
        AuthTabIntent.RESULT_VERIFICATION_FAILED,
        AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT,
        -> {
            val reason =
                if (resultCode == AuthTabIntent.RESULT_VERIFICATION_FAILED) {
                    "콜백 주소 소유 확인 실패"
                } else {
                    "콜백 주소 소유 확인 시간 초과"
                }
            authorizationUrl?.let { AuthTabOutcome.Reopen(it, reason) }
                ?: AuthTabOutcome.Failed("$reason — 다시 열 인증 주소가 없다")
        }
        else -> AuthTabOutcome.Failed("알 수 없는 결과 $resultCode")
    }
