package com.soma369.laimory.core.domain.model.collection

/**
 * 알림 원문에서 보호 대상 정보를 걸러내는 순수 정책.
 *
 * 수집 판정([NotificationFilter])보다 먼저 실행되며 클릭·앱 allowlist·키워드로 우회할 수 없다.
 * 판정 순서는 `전체 제외(마스킹 전 원문 기준) → 부분 마스킹 → 빈 콘텐츠 제외`다.
 *
 * 대화 알림은 원칙적으로 전체 제외지만 기업 발송 문자는 예외로 통과시킨다 — 결제 승인·택배·예약
 * 확인이 앱 푸시가 아니라 문자로 오는 비중이 크기 때문이다.
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
        // 문맥어와 값이 제목·본문에 나뉘어 있을 수 있어 판정은 합친 텍스트로 한다.
        val joined = content.joined()
        if (joined.isBlank()) return null
        if (signals.isMessage && !isBusinessMessage(joined)) return null
        if (isFullyExcluded(joined)) return null

        return NotificationContent(
            title = content.title?.mask(joined),
            text = content.text?.mask(joined),
        ).takeIf { it.joined().isNotBlank() }
    }
}

private fun NotificationContent.joined(): String = listOfNotNull(title, text).joinToString(" ")

/**
 * 기업 발송 문자인지 본다.
 *
 * 결제 승인·택배·예약 확인은 앱 푸시가 아니라 문자로 오는 비중이 크다. 카드사 앱을 쓰지 않는
 * 사용자는 승인 알림이 전부 문자고, 병원·미용실·식당 예약 확인은 앱 자체가 없다. 대화 알림을
 * 통째로 버리면 이 생활 이벤트가 함께 사라진다.
 *
 * `[Web발신]` 은 통신사가 웹 기반 발송 시스템에서 나간 문자에 붙이는 표기다. 법정 표기가 아니라
 * 100% 보장되지 않지만 **틀리는 방향이 안전하다** — 표기가 없는 기업 문자는 놓칠 뿐이고, 개인이
 * 웹발신으로 문자를 보내는 일은 드물어 사적 대화가 통과하는 쪽은 잘 일어나지 않는다.
 *
 * 통과해도 이후 규칙은 그대로 적용된다 — 인증번호·고유식별정보 전체 제외, 전화·계좌 마스킹,
 * 그리고 수집 판정의 `(광고)` 표기 제외. 여기서 푸는 것은 "대화 알림이라 무조건 버린다" 하나뿐이다.
 *
 * `[국제발신]`·`[국외발신]` 은 받지 않는다. 생활 이벤트보다 스팸 비중이 크다.
 */
private fun isBusinessMessage(text: String): Boolean = BUSINESS_MESSAGE_MARKER.containsMatchIn(text)

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
 * 비밀번호 문맥 바로 뒤에 비밀값으로 보이는 토큰이 오면 값 전달로 본다.
 *
 * 문맥과 값 사이에는 조사·구분자·짧은 연결어만 허용한다 — "비밀번호 변경 완료 2026-08-19"
 * 처럼 상태 문구를 사이에 두고 숫자가 나오는 알림은 값 전달이 아니다.
 */
private fun containsPasswordDelivery(text: String): Boolean =
    PASSWORD_DELIVERY.findAll(text).any { match -> match.groupValues[1].looksLikeSecretValue() }

/**
 * 비밀값으로 보이는 토큰인지 본다. 숫자, 비밀번호용 기호, 낱말 중간의 대문자 중 하나라도
 * 있으면 값으로 판정한다.
 *
 * "password reset", "Your password has been changed" 처럼 상태를 알리는 영어 단어를 값으로
 * 오인하지 않기 위한 기준이다. 모두 소문자이고 숫자·기호가 없는 단순 비밀번호는 이 기준으로
 * 잡지 못하는 알려진 한계가 있다.
 */
private fun String.looksLikeSecretValue(): Boolean =
    any(Char::isDigit) ||
        any { it in PASSWORD_VALUE_SYMBOLS } ||
        (1 until length).any { this[it].isUpperCase() && this[it - 1].isLowerCase() }

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
 * 걸리지 않도록 좁은 규칙부터 적용한다. 전화번호는 계좌번호보다 먼저 둔다. 계좌 규칙은
 * 자릿수만 보므로 `010-1234-5678`도 계좌 형식에 걸리는데, 은행 알림에 함께 실린 연락처를
 * [ACCOUNT_TOKEN]으로 바꾸면 안 되기 때문이다.
 */
private fun String.mask(context: String): String =
    replace(EMAIL, EMAIL_TOKEN)
        .maskCardNumbers()
        .replace(MOBILE_NUMBER, PHONE_TOKEN)
        .replace(LANDLINE_NUMBER, PHONE_TOKEN)
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

/** `비밀번호: passWORD!`, `비밀번호가 123456으로`, `password is SecretWord!` 처럼 문맥 뒤에 값이 바로 붙는 형태. */
private val PASSWORD_DELIVERY =
    Regex(
        """(?:비밀번호|password)\s*(?:는|은|이|가|를|을|:|：|=|is|to)?\s*([A-Za-z0-9!@#%^&*_+=?~-]{4,20})""",
        RegexOption.IGNORE_CASE,
    )

/** 비밀번호에 흔한 기호. 문장에서 자주 쓰이는 `.` `,` `-` 는 값 신호로 보지 않는다. */
private const val PASSWORD_VALUE_SYMBOLS = "!@#%^&*_+=?~"

private val RESIDENT_REGISTRATION_NUMBER = Regex("""(?<!\d)\d{6}-[1-4]\d{6}(?!\d)""")
private val FOREIGNER_REGISTRATION_NUMBER = Regex("""(?<!\d)\d{6}-[5-8]\d{6}(?!\d)""")
private val PASSPORT_CONTEXT = Regex("""여권\s*번호|passport\s+number""", RegexOption.IGNORE_CASE)
private val PASSPORT_NUMBER = Regex("""(?<![A-Za-z\d])[A-Z]{1,2}\d{7,8}(?![A-Za-z\d])""")
private val DRIVER_LICENSE_CONTEXT = Regex("""운전\s*면허\s*번호|면허\s*번호""")
private val DRIVER_LICENSE_NUMBER = Regex("""(?<!\d)\d{2}-\d{2}-\d{6}-\d{2}(?!\d)""")

private val MEDICAL_EXAM_CONTEXT = Regex("""진단|검사|검진|판독|소견""")
private val MEDICAL_RESULT_VALUE = Regex("""양성|음성|정상\s*범위|이상\s*소견|수치""")

/**
 * 전화번호 앞 경계. 앞선 숫자 마디에 이어 붙은 숫자열을 전화번호로 보지 않는다.
 *
 * `-` 자체를 거부하면 `연락처-010-1234-5678`, `문의-031-1234-5678` 처럼 라벨을 하이픈으로
 * 붙인 실제 번호가 마스킹되지 않는다. 숫자 뒤에 붙은 하이픈만 거부한다.
 *
 * - `1234-031-1234-5678` → 앞 마디가 숫자라 전화번호로 보지 않는다.
 * - `고객센터-02-1234-5678` → 앞이 라벨이라 치환한다.
 */
private const val NUMBER_SEGMENT_BOUNDARY = """(?<!\d)(?<!\d-)"""

/** 통신사 웹발신 표기. 대괄호 안 공백과 대소문자를 허용한다. */
private val BUSINESS_MESSAGE_MARKER = Regex("""\[\s*web\s*발신\s*]""", RegexOption.IGNORE_CASE)

private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
private val CARD_NUMBER = Regex("""(?<!\d)(?:\d{4}[- ]?){3}\d{4}(?!\d)""")

/**
 * 휴대전화번호. 앞뒤에 숫자 구간이 더 붙으면 매칭하지 않는다 —
 * 기업은행 계좌 `010-1234567-01` 처럼 같은 접두로 시작하는 번호를 부분 치환하지 않기 위해서다.
 *
 * 앞 경계는 [NUMBER_SEGMENT_BOUNDARY] 를 쓴다.
 */
private val MOBILE_NUMBER = Regex("""${NUMBER_SEGMENT_BOUNDARY}01[016789]-?\d{3,4}-?\d{4}(?!-?\d)""")

/**
 * 지역번호가 붙은 유선번호. `02-123-4567`, `031-1234-5678`, `0212345678` 을 모두 잡는다.
 *
 * 지역번호는 실제 할당된 집합만 허용한다 — `0[2-6]\d?` 처럼 열어 두면 `034`, `045` 같은
 * 미할당 번호대의 주문·상품 번호까지 전화번호로 치환한다.
 *
 * | 번호대 | 지역 |
 * | --- | --- |
 * | 02 | 서울 |
 * | 031~033 | 경기·인천·강원 |
 * | 041~044 | 충남·대전·충북·세종 |
 * | 051~055 | 부산·울산·대구·경북·경남 |
 * | 061~064 | 전남·광주·전북·제주 |
 *
 * 앞뒤에 숫자 구간이 더 붙으면 매칭하지 않는다([NUMBER_SEGMENT_BOUNDARY]) —
 * `031-1234-5678-91` 을 `[전화번호]-91` 로 부분 치환하면 남은 마디가 그대로 새어 나간다.
 *
 * `0` 시작을 강제해 두 부류는 애초에 들어오지 않는다.
 * - `1588-0000` 같은 `15xx`·`16xx`·`18xx` 전국대표번호는 사업자 전용 번호대라 개인을
 *   식별하지 않고, 어디였는지를 남기는 생활 맥락이라 유지한다.
 * - 국내 은행 계좌번호는 첫 마디가 `110-`·`301-`·`1002-`·`3333-` 로 시작해 계좌 규칙과 갈린다.
 *
 * 지역번호 없는 `1234-5678` 은 주문·예약번호와 형식이 같아 다루지 않는다.
 * 070 인터넷전화, `0503` 안심번호, `+82` 국제 표기도 1차 범위 밖이다.
 */
private val LANDLINE_NUMBER =
    Regex("""${NUMBER_SEGMENT_BOUNDARY}0(?:2|3[1-3]|4[1-4]|5[1-5]|6[1-4])-?\d{3,4}-?\d{4}(?!-?\d)""")
private val BANK_ACCOUNT_CONTEXT = Regex("""계좌|입금|출금|이체""")

/**
 * 계좌번호. 농협·새마을처럼 마디가 넷인 형식(`301-1234-5678-91`)까지 한 번에 잡는다 —
 * 앞 세 마디만 치환하면 남은 마디가 `[계좌번호]-91` 로 새어 나간다.
 */
private val BANK_ACCOUNT_NUMBER =
    Regex("""(?<!\d)\d{2,6}-\d{2,6}-\d{2,8}(?:-\d{1,4})?(?!\d)|(?<!\d)\d{10,14}(?!\d)""")

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
