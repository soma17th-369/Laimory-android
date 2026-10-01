package com.soma369.laimory.core.domain.model.inquiry

/**
 * 문의 입력 규칙. 서버 검증(Bean Validation · 첨부 상한)과 같은 기준을 앱이 먼저 건다.
 *
 * 서버가 막는 값을 보내 실패시키는 것보다, 보내기 버튼을 누를 수 없게 두는 편이 무엇이 모자란지
 * 바로 보인다. 서버가 정본이므로 여기가 더 느슨해지면 안 된다.
 */
object InquiryInputRules {
    const val EMAIL_MAX_LENGTH = 255
    const val TITLE_MAX_LENGTH = 100
    const val DESCRIPTION_MAX_LENGTH = 2_000
    const val MAX_ATTACHMENTS = 3

    // 서버 `@Email` 보다 조금 좁다 — 도메인에 점이 없는 주소는 답장을 받을 수 없어 앱이 먼저 거른다.
    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.length <= EMAIL_MAX_LENGTH && EMAIL_PATTERN.matches(trimmed)
    }

    /** 공백만은 안 되고, 서버처럼 앞뒤 공백을 뺀 길이로 잰다. */
    fun isValidTitle(title: String): Boolean {
        val trimmed = title.trim()
        return trimmed.isNotEmpty() && trimmed.length <= TITLE_MAX_LENGTH
    }

    /**
     * 입력 중인 제목을 [TITLE_MAX_LENGTH] 에 맞춰 자른다. 규칙과 같이 앞뒤 공백을 뺀 길이로 잰다 —
     * 원문 길이로 자르면 앞에 공백을 붙여 넣은 100자 제목의 끝 글자가 잘린다. 앞 공백은 그대로 두고
     * 글자 부분만 100자로 자른다(뒤 공백은 보낼 때 빠진다).
     */
    fun limitTitle(title: String): String {
        val leading = title.takeWhile(Char::isWhitespace)
        val rest = title.drop(leading.length)
        if (rest.trimEnd().length <= TITLE_MAX_LENGTH) return title
        return leading + rest.take(TITLE_MAX_LENGTH)
    }

    /** 공백만은 안 되고, 줄바꿈을 포함한 원문 길이로 잰다(서버와 같다). */
    fun isValidDescription(description: String): Boolean = description.isNotBlank() && description.length <= DESCRIPTION_MAX_LENGTH
}
