package com.soma369.laimory.core.ui.greeting

/**
 * 닉네임 인사말을 조각으로 만든다.
 *
 * 홈과 온보딩 소개가 함께 쓴다 — 같은 사용자를 두 화면이 다르게 부르면 안 된다.
 *
 * 색만 다른 한 문장이므로 화면에서는 여러 `Text`로 쪼개지 않고 한 `AnnotatedString`으로 합친다 —
 * 나눠 두면 접근성 서비스가 `안녕하세요,` / `김소마` / `님` 을 따로 읽는다.
 *
 * 닉네임이 없으면 `님`까지 함께 빠진다. 조회가 늦거나 실패해도 이 문구를 그대로 쓰므로 화면이
 * 비어 보이지 않는다.
 */
fun nicknameGreetingSegments(nickname: String?): List<GreetingSegment> {
    val displayName =
        nickname?.trim()?.takeIf(String::isNotEmpty)
            ?: return listOf(GreetingSegment(GREETING_WITHOUT_NAME, GreetingEmphasis.NORMAL))
    return listOf(
        GreetingSegment(GREETING_PREFIX, GreetingEmphasis.NORMAL),
        GreetingSegment(displayName, GreetingEmphasis.NICKNAME),
        GreetingSegment(GREETING_SUFFIX, GreetingEmphasis.NORMAL),
    )
}

private const val GREETING_WITHOUT_NAME = "안녕하세요"
private const val GREETING_PREFIX = "안녕하세요, "
private const val GREETING_SUFFIX = "님"
