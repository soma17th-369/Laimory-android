package com.soma369.laimory.core.domain.navigation

/**
 * 앱 내 이동·딥링크 공통 내비게이션 단위.
 *
 * - [path]: 이동 대상 페이지 식별자. 앱 내 이동과 deep-link 양쪽에서 같은 값을 쓴다. 예: "/home"
 * - [args]: 페이지 인자. 모두 String 으로 직렬화된 형태로 전달하고, 복잡 타입은 JSON 문자열로 인코딩한다.
 */
data class NavRoute(
    val path: String,
    val args: Map<String, String> = emptyMap(),
)

/**
 * 각 화면이 정의하는 의미 수준 목적지 마커.
 *
 * feature 는 자기 [Page] 객체를 정의하고 [toRoute] 에서 자기 인자를 [NavRoute] 로 직렬화한다.
 * domain 은 Nav3·`NavController`·route string 을 알지 않는다 — 의미 수준 식별자만 안다.
 */
interface Page {
    fun toRoute(): NavRoute
}
