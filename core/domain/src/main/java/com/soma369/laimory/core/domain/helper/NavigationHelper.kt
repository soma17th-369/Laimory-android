package com.soma369.laimory.core.domain.helper

import com.soma369.laimory.core.domain.navigation.Page

/**
 * 도메인이 앱 이동을 발행하는 의미 수준 포트. 전진/후진 모두 이 포트로 표현한다.
 *
 * presentation 구현체가 이동을 실제 backStack 조작으로 매핑한다.
 * domain 은 Nav3·`NavController` 를 알지 않는다 — 의미 수준 [Page]/`NavRoute` 만 안다.
 */
interface NavigationHelper {
    fun navigateTo(page: Page)

    /** 로그인·로그아웃처럼 이전 화면으로 돌아가면 안 되는 전환에서 백스택 루트를 교체한다. */
    fun replaceRoot(page: Page)

    fun navigateToBack()
}
