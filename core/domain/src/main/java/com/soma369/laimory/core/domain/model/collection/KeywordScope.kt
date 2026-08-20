package com.soma369.laimory.core.domain.model.collection

/**
 * 기본 키워드가 걸리는 범위. 도메인 단위로 키워드와 앱을 짝지어 둔다.
 *
 * 키워드는 도메인 사이에 겹칠 수 있다 — `취소` 는 전 도메인, `도착` 은 택배·배달·이동에
 * 함께 쓰인다. 어느 한 scope 만 통과하면 수집한다.
 *
 * 키워드마다 앱을 나열하지 않는 이유는 같은 앱 집합이 여러 번 복제되기 때문이다. 도메인
 * 그룹은 scope 를 잘게 쪼개면 키워드별 매핑과 같아지므로, 특정 키워드에만 다른 앱 집합을
 * 물려야 할 때 구조를 바꾸지 않고 scope 를 더하면 된다.
 */
data class KeywordScope(
    val keywords: Set<String>,
    val apps: Set<AppMatch>,
) {
    /** [packageName] 이 이 도메인의 앱이고 [content] 에 이 도메인의 키워드가 있는지. */
    fun matches(
        packageName: String,
        content: String,
    ): Boolean =
        apps.any { it.covers(packageName) } &&
            keywords.containsKeywordIn(content)
}

/** 빈 키워드는 설정에 남아 있더라도 일치 조건에서 제외한다. */
internal fun Set<String>.containsKeywordIn(content: String): Boolean = any { it.isNotBlank() && content.contains(it, ignoreCase = true) }
