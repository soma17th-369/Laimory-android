package com.soma369.laimory.core.domain.model.collection

/**
 * 알림 원문에서 보호 대상 정보를 걸러내는 순수 정책.
 *
 * 수집 판정([NotificationFilter])보다 먼저 실행되며 클릭·앱 allowlist·키워드로 우회할 수 없다.
 * 판정 순서는 `전체 제외(마스킹 전 원문 기준) → 부분 마스킹 → 빈 콘텐츠 제외`다.
 *
 * 규칙은 형식과 문맥어로만 판정한다. 사람 이름·질병명처럼 사전이나 NER 없이는 정확히
 * 가릴 수 없는 값은 대상에 넣지 않는다 — 넓은 규칙은 날짜·금액·주문번호처럼 생활 기록에
 * 필요한 정보를 함께 지운다.
 *
 * Android 프레임워크에 의존하지 않으므로 합성 데이터 JVM 테스트로 검증한다.
 */
class NotificationPrivacyPolicy {
    /**
     * [content]에서 전체 제외 대상을 걸러내고 남은 개인 식별정보를 의미 보존 토큰으로 치환한다.
     *
     * @param signals 리스너 경계에서 얻은 구조 신호. 로컬 저장분 재적용처럼 복원할 수 없는
     *   경계에서는 [NotificationSignals.UNAVAILABLE]을 넘긴다.
     * @return 저장·전송해도 되는 정제 결과. 전체 제외 대상이거나 정제 후 제목·본문이 모두
     *   비면 null 이며, 호출부는 저장하지 않는다.
     */
    fun sanitize(
        content: NotificationContent,
        signals: NotificationSignals,
    ): NotificationContent? {
        if (signals.isMessage) return null

        // 문맥어와 값이 제목·본문에 나뉘어 있을 수 있어 판정은 합친 텍스트로 한다.
        val joined = content.joined()
        if (joined.isBlank()) return null
        if (isFullyExcluded(joined)) return null

        return NotificationContent(
            title = content.title?.mask(joined),
            text = content.text?.mask(joined),
        ).takeIf { it.joined().isNotBlank() }
    }
}

private fun NotificationContent.joined(): String = listOfNotNull(title, text).joinToString(" ")

private fun isFullyExcluded(text: String): Boolean =
    containsAuthenticationSecret(text) ||
        containsUniqueIdentifier(text) ||
        containsMedicalResult(text)

/**
 * 인증정보. 인증번호 문맥과 4~8자리 숫자가 함께 있을 때만 제외한다 —
 * "본인인증이 완료되었습니다"처럼 코드 없는 완료 알림은 생활 이벤트라 유지한다.
 */
private fun containsAuthenticationSecret(text: String): Boolean =
    (AUTH_CODE_CONTEXT.containsMatchIn(text) && AUTH_CODE_VALUE.containsMatchIn(text)) ||
        ACCOUNT_SECRET.containsMatchIn(text) ||
        containsPasswordDelivery(text)

/**
 * 비밀번호 문맥 바로 뒤에 숫자를 포함한 값이 따라오면 비밀값 전달로 본다.
 *
 * "비밀번호가 변경되었습니다", "비밀번호를 재설정하세요" 처럼 조사와 한글이 이어지는 상태
 * 알림은 값 패턴에 걸리지 않아 유지된다.
 */
private fun containsPasswordDelivery(text: String): Boolean =
    PASSWORD_DELIVERY.findAll(text).any { match -> match.groupValues[1].any(Char::isDigit) }

/**
 * 고유식별정보. 주민·외국인등록번호는 형식만으로 판정하고, 여권·운전면허번호는 형식이
 * 주문번호·상품코드와 겹쳐 문맥어가 함께 있을 때만 제외한다.
 *
 * 문맥어가 생략된 여권·면허번호는 1차 규칙으로 잡지 못하는 알려진 한계다.
 */
private fun containsUniqueIdentifier(text: String): Boolean =
    RESIDENT_REGISTRATION_NUMBER.containsMatchIn(text) ||
        FOREIGNER_REGISTRATION_NUMBER.containsMatchIn(text) ||
        (PASSPORT_CONTEXT.containsMatchIn(text) && PASSPORT_NUMBER.containsMatchIn(text)) ||
        (DRIVER_LICENSE_CONTEXT.containsMatchIn(text) && DRIVER_LICENSE_NUMBER.containsMatchIn(text))

/**
 * 상세 진단·검사 결과. 검사 문맥과 결과 값이 함께 있을 때만 제외한다 —
 * 병원명과 일시만 있는 진료 예약·접수·결제 알림은 생활 이벤트라 유지한다.
 */
private fun containsMedicalResult(text: String): Boolean =
    MEDICAL_EXAM_CONTEXT.containsMatchIn(text) && MEDICAL_RESULT_VALUE.containsMatchIn(text)

/**
 * 개인 식별정보를 의미 보존 토큰으로 치환한다.
 *
 * [context]는 제목·본문을 합친 판정용 텍스트다. 계좌번호처럼 문맥어가 다른 필드에 있는
 * 경우를 놓치지 않기 위해 값과 문맥을 분리해서 받는다.
 *
 * 순서가 결과를 바꾼다 — 이메일 로컬파트와 카드번호가 전화번호·계좌번호 형식에 먼저
 * 걸리지 않도록 좁은 규칙부터 적용한다.
 */
private fun String.mask(context: String): String =
    replace(EMAIL, EMAIL_TOKEN)
        .maskCardNumbers()
        .replace(PHONE_NUMBER, PHONE_TOKEN)
        .maskBankAccounts(context)
        .maskRoadAddresses()
        .replace(LOT_ADDRESS, ADDRESS_TOKEN)

/** 카드번호 형식이어도 Luhn 검증을 통과할 때만 치환한다 — 주문번호·운송장번호 오탐을 막는다. */
private fun String.maskCardNumbers(): String =
    CARD_NUMBER.replace(this) { match ->
        if (match.value.filter(Char::isDigit).isLuhnValid()) CARD_TOKEN else match.value
    }

/**
 * 계좌번호는 형식만으로 주문·예약번호와 구분되지 않아 계좌 문맥어가 있을 때만 치환한다.
 *
 * 문맥이 있어도 자릿수가 계좌 범위에 못 미치면 남긴다 — `2026-08-19` 같은 날짜가
 * 구분자 형식만으로는 계좌번호와 같은 모양이기 때문이다.
 */
private fun String.maskBankAccounts(context: String): String {
    if (!BANK_ACCOUNT_CONTEXT.containsMatchIn(context)) return this
    return BANK_ACCOUNT_NUMBER.replace(this) { match ->
        if (match.value.count(Char::isDigit) >= BANK_ACCOUNT_MIN_DIGITS) ACCOUNT_TOKEN else match.value
    }
}

/**
 * 도로명 + 건물번호를 치환한다. 시·군·구와 장소명은 남긴다.
 *
 * `별도로 30`, `추가로 20%` 처럼 도로명과 형태가 같은 부사는 규칙만으로 구분할 수 없어
 * 명시 목록으로 제외한다.
 */
private fun String.maskRoadAddresses(): String =
    ROAD_ADDRESS.replace(this) { match ->
        if (ADVERB_LOOKALIKE.matches(match.groupValues[1])) match.value else ADDRESS_TOKEN
    }

private fun String.isLuhnValid(): Boolean {
    var sum = 0
    var doubled = false
    for (index in indices.reversed()) {
        var digit = this[index] - '0'
        if (doubled) {
            digit *= 2
            if (digit > 9) digit -= 9
        }
        sum += digit
        doubled = !doubled
    }
    return sum % 10 == 0
}

private const val PHONE_TOKEN = "[전화번호]"
private const val EMAIL_TOKEN = "[이메일]"
private const val CARD_TOKEN = "[카드번호]"
private const val ACCOUNT_TOKEN = "[계좌번호]"
private const val ADDRESS_TOKEN = "[상세주소]"

private val AUTH_CODE_CONTEXT =
    Regex("""인증\s*번호|인증\s*코드|보안\s*코드|OTP|verification\s+code""", RegexOption.IGNORE_CASE)
private val AUTH_CODE_VALUE = Regex("""(?<!\d)\d{4,8}(?!\d)""")
private val ACCOUNT_SECRET =
    Regex(
        """임시\s*비밀번호|초기\s*비밀번호|temporary\s+password|계정\s*복구\s*코드|복구\s*코드|recovery\s+code""",
        RegexOption.IGNORE_CASE,
    )

/** `비밀번호: 123456`, `새 비밀번호 abcD!23` 처럼 문맥 뒤에 값이 바로 붙는 형태. */
private val PASSWORD_DELIVERY =
    Regex("""(?:비밀번호|password)\s*(?:는|은|:|：|=|is)?\s*([A-Za-z0-9!@#%&*_+.\-]{4,20})""", RegexOption.IGNORE_CASE)

private val RESIDENT_REGISTRATION_NUMBER = Regex("""(?<!\d)\d{6}-[1-4]\d{6}(?!\d)""")
private val FOREIGNER_REGISTRATION_NUMBER = Regex("""(?<!\d)\d{6}-[5-8]\d{6}(?!\d)""")
private val PASSPORT_CONTEXT = Regex("""여권\s*번호|passport\s+number""", RegexOption.IGNORE_CASE)
private val PASSPORT_NUMBER = Regex("""(?<![A-Za-z\d])[A-Z]{1,2}\d{7,8}(?![A-Za-z\d])""")
private val DRIVER_LICENSE_CONTEXT = Regex("""운전\s*면허\s*번호|면허\s*번호""")
private val DRIVER_LICENSE_NUMBER = Regex("""(?<!\d)\d{2}-\d{2}-\d{6}-\d{2}(?!\d)""")

private val MEDICAL_EXAM_CONTEXT = Regex("""진단|검사|검진|판독|소견""")
private val MEDICAL_RESULT_VALUE = Regex("""양성|음성|정상\s*범위|이상\s*소견|수치""")

private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
private val CARD_NUMBER = Regex("""(?<!\d)(?:\d{4}[- ]?){3}\d{4}(?!\d)""")
private val PHONE_NUMBER = Regex("""(?<!\d)01[016789]-?\d{3,4}-?\d{4}(?!\d)""")
private val BANK_ACCOUNT_CONTEXT = Regex("""계좌|입금|출금|이체""")
private val BANK_ACCOUNT_NUMBER =
    Regex("""(?<!\d)\d{2,6}-\d{2,6}-\d{2,8}(?!\d)|(?<!\d)\d{10,14}(?!\d)""")

/** 국내 계좌번호 자릿수 하한. 이보다 짧은 구분자 숫자열은 날짜·주문번호로 본다. */
private const val BANK_ACCOUNT_MIN_DIGITS = 10

/**
 * 도로명 + 건물번호. 건물번호 뒤에는 주소에 붙는 조사만 허용해 수량·기간 표현과 가른다.
 *
 * - `테헤란로 152에 도착`, `테헤란로 5` → 주소로 본다.
 * - `그대로 30분`, `추가로 20%`, `별도로 30,000원` → 단위·천 단위 구분이라 주소로 보지 않는다.
 */
private val ROAD_ADDRESS =
    Regex(
        """([가-힣]{2,10})(?:대로|로|길)\s?\d{1,5}(?:-\d{1,5})?(?!,\d{3})""" +
            """(?:(?![가-힣\d%])|(?=에서|에|으로|로|까지|앞|인근|근처))""",
    )
private val LOT_ADDRESS = Regex("""(?<!\d)\d{1,5}(?:-\d{1,5})?번지""")

/** 도로명으로 오인되는 부사의 어간. 새 사례가 나오면 여기에 추가한다. */
private val ADVERB_LOOKALIKE =
    Regex("""별도|참고|실제|추가|무료|자동으|수동으|대체|그대|이대|임의|차례|의도적으|상대적으|정기적으|일시적으""")
