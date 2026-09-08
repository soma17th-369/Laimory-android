package com.soma369.laimory.core.util.logging

/**
 * 원격으로 내보내기 전에 **메시지를 걷어낸** 예외 사본.
 *
 * 예외를 throwable 인자로 옮기는 것만으로는 원문이 사라지지 않는다. 크래시 리포팅 SDK 는
 * `localizedMessage` 와 cause 를 읽어 리포트에 그대로 싣는다. 그리고 그 메시지는 우리가 다 쓰지
 * 않는다 — 안드로이드 예외에는 파일 경로와 content URI 가, 우리 예외에는 사진 주소나 서버가 준
 * 문자열이 들어갈 수 있고, 서버 문자열은 화면에 보여 주려고 일부러 보존하는 값이다.
 *
 * 그래서 마지막 경계에서 한 번 걷는다. 안전한 것만 남긴다.
 *
 * - **메시지** — 원본 예외의 클래스 이름. 코드 식별자라 사용자 데이터가 아니고, 리포트에서 어떤
 *   예외였는지 그대로 읽힌다.
 * - **스택** — 원본 그대로. 어디서 났는지가 진단의 본체이고 사용자 데이터를 담지 않는다.
 * - **cause** — 같은 방식으로 걷어서 잇는다.
 *
 * 예외 생성 지점을 하나씩 고치는 방식은 택하지 않았다. 새로 생기는 자리마다 빠뜨리고, 무엇보다
 * 안드로이드와 서드파티 예외의 메시지는 우리가 정할 수 없다.
 */
class RedactedThrowable private constructor(
    typeName: String,
    cause: RedactedThrowable?,
) : Throwable(typeName, cause) {
    companion object {
        /** cause 사슬을 따라가는 최대 깊이. 서로를 가리키는 사슬에서 멈추기 위한 것이다. */
        private const val MAX_CAUSE_DEPTH = 5

        fun of(throwable: Throwable): RedactedThrowable = redact(throwable, MAX_CAUSE_DEPTH)

        private fun redact(
            throwable: Throwable,
            remainingDepth: Int,
        ): RedactedThrowable {
            val cause =
                throwable.cause
                    ?.takeIf { remainingDepth > 0 && it !== throwable }
                    ?.let { redact(it, remainingDepth - 1) }
            return RedactedThrowable(throwable::class.java.name, cause)
                .apply { stackTrace = throwable.stackTrace }
        }
    }
}
