package com.soma369.laimory

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 소셜 로그인 인증 페이지를 연다.
 *
 * 실행은 [MainActivity] 가 맡는다. Auth Tab 결과는 실행을 등록한 곳으로 돌아오는데, 화면(컴포저블)이
 * 등록하면 그 화면이 다시 그려지지 않는 한 결과를 받을 자리가 없다. Activity 는 재생성·프로세스
 * 종료 뒤에도 `onCreate` 에서 같은 순서로 다시 등록하므로 결과가 유실되지 않는다.
 */
internal interface AuthorizationLauncher {
    /** 인증 페이지를 연다. 열 브라우저가 없으면 false. */
    fun launch(url: String): Boolean

    /**
     * 마지막 [launch] 뒤로 인증 페이지를 다시 열었으면 true 를 돌려주고 표시를 지운다.
     *
     * Auth Tab 이 콜백 주소 소유 확인에 실패하면 같은 페이지를 일반 Custom Tab 으로 다시 여는데, 그 사이
     * 앱이 잠깐 앞으로 나온다. 로그인 화면이 이것을 "콜백 없이 돌아왔다"로 읽으면 진행 중인 로그인을
     * 취소하고, 다시 연 탭에서 돌아온 콜백은 교환할 시도가 없어 버려진다. 결과는 복귀(onResume)보다
     * 먼저 전달되므로 화면은 복귀를 판정할 때 이 값을 읽으면 된다.
     */
    fun consumeReopen(): Boolean
}

internal val LocalAuthorizationLauncher =
    staticCompositionLocalOf<AuthorizationLauncher> {
        error("AuthorizationLauncher 가 제공되지 않았다 — MainActivity 가 CompositionLocalProvider 로 넣는다")
    }
