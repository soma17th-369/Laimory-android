package com.soma369.laimory.core.domain.model.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationPrivacyPolicyTest {
    private val policy = NotificationPrivacyPolicy()

    private fun sanitize(
        title: String? = null,
        text: String? = null,
        isMessage: Boolean = false,
    ): NotificationContent? =
        policy.sanitize(
            content = NotificationContent(title = title, text = text),
            signals = NotificationSignals(isMessage = isMessage),
        )

    // --- 전체 제외: 인증·계정 비밀 ---

    @Test
    fun `인증번호 문맥과 숫자가 함께 있으면 저장하지 않는다`() {
        assertNull(sanitize(title = "인증번호 안내", text = "[123456] 인증번호를 입력해주세요"))
        assertNull(sanitize(text = "인증 번호 8842 입니다"))
        assertNull(sanitize(text = "Your verification code is 483920"))
    }

    @Test
    fun `코드가 없는 인증 완료 알림은 생활 이벤트로 유지한다`() {
        val result = sanitize(title = "본인인증 완료", text = "본인인증이 완료되었습니다")

        assertEquals("본인인증이 완료되었습니다", result?.text)
    }

    @Test
    fun `임시 비밀번호와 계정 복구 코드는 숫자와 무관하게 저장하지 않는다`() {
        assertNull(sanitize(text = "임시 비밀번호가 발급되었습니다"))
        assertNull(sanitize(text = "복구 코드를 안전한 곳에 보관하세요"))
    }

    @Test
    fun `비밀번호 변경 안내는 비밀값 전달이 아니므로 유지한다`() {
        assertNotNull(sanitize(text = "비밀번호가 변경되었습니다"))
    }

    // --- 전체 제외: 고유식별정보 ---

    @Test
    fun `주민등록번호와 외국인등록번호는 형식만으로 저장하지 않는다`() {
        assertNull(sanitize(text = "제출 서류에 900101-1234567 확인이 필요합니다"))
        assertNull(sanitize(text = "등록번호 900101-5234567 확인"))
    }

    @Test
    fun `여권과 운전면허번호는 문맥어가 함께 있을 때만 저장하지 않는다`() {
        assertNull(sanitize(text = "여권번호 M12345678 확인이 완료되었습니다"))
        assertNull(sanitize(text = "운전면허번호 11-22-334455-66 등록 완료"))
        // 문맥어가 없으면 상품코드·주문번호와 구분할 수 없어 유지한다(알려진 한계).
        assertNotNull(sanitize(text = "상품코드 M12345678 발송 완료"))
    }

    @Test
    fun `주문번호와 운송장번호는 고유식별정보로 보지 않는다`() {
        val result = sanitize(text = "주문번호 20260819-0012 운송장번호 123456789012 배송 시작")

        assertEquals("주문번호 20260819-0012 운송장번호 123456789012 배송 시작", result?.text)
    }

    // --- 전체 제외: 상세 의료정보 ---

    @Test
    fun `검사 문맥과 결과 값이 함께 있으면 저장하지 않는다`() {
        assertNull(sanitize(title = "검사 결과 안내", text = "코로나 검사 결과는 양성입니다"))
        assertNull(sanitize(text = "건강검진 결과 수치가 정상 범위를 벗어났습니다"))
    }

    @Test
    fun `병원명과 일시만 있는 진료 예약은 유지한다`() {
        val result = sanitize(title = "서울내과", text = "8월 20일 14:30 진료 예약이 확정되었습니다")

        assertEquals("8월 20일 14:30 진료 예약이 확정되었습니다", result?.text)
    }

    // --- 전체 제외: 메시지 원문 ---

    @Test
    fun `대화 알림은 본문과 무관하게 저장하지 않는다`() {
        assertNull(sanitize(title = "민우", text = "내일 7시에 보자", isMessage = true))
    }

    @Test
    fun `구조 신호를 알 수 없는 경계에서는 텍스트 규칙만 적용한다`() {
        val message = NotificationContent(title = "민우", text = "내일 7시에 보자")

        // 로컬 저장분에는 구조 신호가 없어 대화 알림을 되짚을 수 없다(알려진 한계).
        assertNotNull(policy.sanitize(message, NotificationSignals.UNAVAILABLE))
        // 텍스트로 판정할 수 있는 규칙은 그대로 다시 적용된다.
        assertNull(policy.sanitize(NotificationContent(null, "인증번호 123456"), NotificationSignals.UNAVAILABLE))
    }

    // --- 부분 마스킹 ---

    @Test
    fun `전화번호와 이메일을 의미 보존 토큰으로 치환한다`() {
        val result = sanitize(text = "기사님 010-1234-5678 / 문의 help@laimory.com")

        assertEquals("기사님 [전화번호] / 문의 [이메일]", result?.text)
    }

    @Test
    fun `카드번호는 Luhn 을 통과할 때만 치환한다`() {
        assertEquals("[카드번호] 승인", sanitize(text = "4242 4242 4242 4242 승인")?.text)
        // 형식만 같고 검증에 실패하는 숫자열은 주문·운송장 번호일 수 있어 남긴다.
        assertEquals("1234-5678-9012-3456 조회", sanitize(text = "1234-5678-9012-3456 조회")?.text)
    }

    @Test
    fun `계좌번호는 계좌 문맥이 있을 때만 치환한다`() {
        assertEquals("입금 계좌 [계좌번호]", sanitize(text = "입금 계좌 110-123-456789")?.text)
        assertEquals("예약번호 110-123-456789", sanitize(text = "예약번호 110-123-456789")?.text)
    }

    @Test
    fun `계좌 문맥이 제목에 있고 번호가 본문에 있어도 치환한다`() {
        val result = sanitize(title = "계좌 이체 완료", text = "1002-345-678901 로 송금했습니다")

        assertEquals("[계좌번호] 로 송금했습니다", result?.text)
    }

    @Test
    fun `계좌 문맥이 있어도 날짜는 계좌번호로 보지 않는다`() {
        val result = sanitize(text = "계좌 이체 완료 2026-08-19 12,345원")

        assertEquals("계좌 이체 완료 2026-08-19 12,345원", result?.text)
    }

    @Test
    fun `도로명 주소와 번지는 치환하고 시군구와 장소명은 남긴다`() {
        assertEquals("서울시 강남구 [상세주소] 도착", sanitize(text = "서울시 강남구 테헤란로 152 도착")?.text)
        assertEquals("성남시 [상세주소] 수령", sanitize(text = "성남시 123-4번지 수령")?.text)
    }

    @Test
    fun `도로명과 형태가 같은 부사는 주소로 보지 않는다`() {
        assertEquals("별도로 30,000원이 청구됩니다", sanitize(text = "별도로 30,000원이 청구됩니다")?.text)
        assertEquals("추가로 20% 할인", sanitize(text = "추가로 20% 할인")?.text)
        assertEquals("새로 5개 상품이 등록되었습니다", sanitize(text = "새로 5개 상품이 등록되었습니다")?.text)
    }

    // --- 비민감 정보 보존 ---

    @Test
    fun `날짜와 시각 금액 수량은 원형을 유지한다`() {
        val original = "2026-08-19 08/19 14:30 결제 12,345원 3개 승인"

        assertEquals(original, sanitize(text = original)?.text)
    }

    @Test
    fun `생활 이벤트 상태 문구는 그대로 남는다`() {
        val original = "주문하신 상품이 출발했습니다. 오늘 도착 예정이에요."

        assertEquals(original, sanitize(text = original)?.text)
    }

    // --- 빈 콘텐츠 ---

    @Test
    fun `제목과 본문이 모두 비면 저장하지 않는다`() {
        assertNull(sanitize())
        assertNull(sanitize(title = "  ", text = "\n"))
    }

    @Test
    fun `제목이나 본문 중 하나만 있어도 저장한다`() {
        assertEquals("배송 출발", sanitize(title = "배송 출발")?.title)
        assertNull(sanitize(title = "배송 출발")?.text)
    }

    // --- 한글 조사 결합 (PR #262 리뷰) ---

    @Test
    fun `번호 뒤에 조사가 붙어도 전체 제외와 마스킹이 동작한다`() {
        // Java 정규식의 \b 는 한글을 단어 문자로 취급해 숫자와 조사 사이에 경계가 생기지 않는다.
        assertNull(sanitize(text = "인증번호 123456입니다"))
        assertNull(sanitize(text = "주민등록번호 900101-1234567입니다"))
        assertNull(sanitize(text = "외국인등록번호 900101-5234567로 확인됨"))
        assertNull(sanitize(text = "여권번호 M12345678입니다"))
        assertNull(sanitize(text = "운전면허번호 11-22-334455-66으로 등록"))
        assertEquals("기사님 [전화번호]로 연락주세요", sanitize(text = "기사님 010-1234-5678로 연락주세요")?.text)
        assertEquals("입금 계좌 [계좌번호]로 송금", sanitize(text = "입금 계좌 110-123-456789로 송금")?.text)
    }

    @Test
    fun `조사가 붙어도 더 긴 숫자열은 인증번호로 보지 않는다`() {
        val original = "인증번호 안내 주문 1234567890123입니다"

        assertEquals(original, sanitize(text = original)?.text)
    }

    // --- 비밀번호 값 전달 (PR #262 리뷰) ---

    @Test
    fun `비밀번호 문맥 뒤에 값이 따라오면 저장하지 않는다`() {
        assertNull(sanitize(text = "비밀번호: 123456"))
        assertNull(sanitize(text = "새 비밀번호 abcD!23 으로 로그인하세요"))
        assertNull(sanitize(text = "Your password is Abc12345"))
    }

    @Test
    fun `값이 없는 비밀번호 상태 알림은 유지한다`() {
        assertEquals("비밀번호가 변경되었습니다", sanitize(text = "비밀번호가 변경되었습니다")?.text)
        assertEquals("비밀번호를 재설정하세요", sanitize(text = "비밀번호를 재설정하세요")?.text)
        assertEquals("비밀번호 변경 안내", sanitize(text = "비밀번호 변경 안내")?.text)
    }

    // --- 도로명 주소 종단 (PR #262 리뷰) ---

    @Test
    fun `건물번호에 조사가 붙거나 한 자리여도 주소로 본다`() {
        // 조사는 lookahead 라 치환 대상에서 빠진다 — 토큰 뒤에 남아 문장 형태가 유지된다.
        assertEquals("[상세주소]에 도착 예정", sanitize(text = "테헤란로 152에 도착 예정")?.text)
        assertEquals("[상세주소]으로 이동", sanitize(text = "세종대로 110으로 이동")?.text)
        assertEquals("[상세주소]", sanitize(text = "테헤란로 5")?.text)
    }

    @Test
    fun `수량과 기간 백분율 금액 표현은 주소로 보지 않는다`() {
        assertEquals("그대로 30분 대기", sanitize(text = "그대로 30분 대기")?.text)
        assertEquals("차례로 12건 처리", sanitize(text = "차례로 12건 처리")?.text)
        assertEquals("임의로 50,000원 청구", sanitize(text = "임의로 50,000원 청구")?.text)
        assertEquals("정기적으로 20% 적립", sanitize(text = "정기적으로 20% 적립")?.text)
        assertEquals("별도로 3 건이 추가됩니다", sanitize(text = "별도로 3 건이 추가됩니다")?.text)
    }
}
