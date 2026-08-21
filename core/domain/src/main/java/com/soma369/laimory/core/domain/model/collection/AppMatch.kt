package com.soma369.laimory.core.domain.model.collection

/**
 * 기본 키워드가 적용될 앱을 가리키는 규칙.
 *
 * 기본은 [Exact] 다. 접두 매칭은 서비스 경계를 보장하지 않는다 — `com.coupang.mobile` 은
 * 다른 도메인에 속한 `com.coupang.mobile.eats` 까지 통과시키고, 부모·자식 관계라 구분자
 * 경계를 요구해도 갈라지지 않는다.
 *
 * 정확한 application ID 는 틀리면 침묵으로 실패하지만 Google Play URL 의 `id` 파라미터로
 * 검증하고 근거를 남길 수 있다. 검증할 수 있는 쪽을 기본으로 둔다.
 */
sealed interface AppMatch {
    /** application ID. [Prefix] 에서는 그 접두다. */
    val id: String

    /** [packageName] 이 이 규칙에 해당하는지. */
    fun covers(packageName: String): Boolean

    /** 검증된 application ID 하나에만 해당한다. */
    data class Exact(override val id: String) : AppMatch {
        override fun covers(packageName: String): Boolean = packageName == id
    }

    /**
     * 같은 서비스의 변형 앱을 실제로 확인했을 때만 쓴다.
     *
     * 구분자 경계를 요구해 `com.example` 이 `com.exampleother` 를 삼키지 않게 한다.
     * 다른 도메인의 [Exact] 를 삼키지 않는지는 테스트로 고정한다.
     */
    data class Prefix(override val id: String) : AppMatch {
        override fun covers(packageName: String): Boolean = packageName == id || packageName.startsWith("$id.")
    }
}
