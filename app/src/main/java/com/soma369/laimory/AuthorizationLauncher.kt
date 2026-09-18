package com.soma369.laimory

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 소셜 로그인 인증 페이지를 연다. 열 브라우저가 없으면 false 를 돌려준다.
 *
 * 실행은 [MainActivity] 가 맡는다. Auth Tab 결과는 실행을 등록한 곳으로 돌아오는데, 화면(컴포저블)이
 * 등록하면 그 화면이 다시 그려지지 않는 한 결과를 받을 자리가 없다. Activity 는 재생성·프로세스
 * 종료 뒤에도 `onCreate` 에서 같은 순서로 다시 등록하므로 결과가 유실되지 않는다.
 */
internal fun interface AuthorizationLauncher {
    fun launch(url: String): Boolean
}

internal val LocalAuthorizationLauncher =
    staticCompositionLocalOf<AuthorizationLauncher> {
        error("AuthorizationLauncher 가 제공되지 않았다 — MainActivity 가 CompositionLocalProvider 로 넣는다")
    }
