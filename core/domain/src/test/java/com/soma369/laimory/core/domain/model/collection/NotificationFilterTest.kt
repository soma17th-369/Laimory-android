package com.soma369.laimory.core.domain.model.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationFilterTest {
    @Test
    fun `클릭한 알림은 필터가 비어 있어도 수집한다`() {
        val reason =
            NotificationFilter().collectReasonFor(
                packageName = "com.example",
                title = null,
                text = null,
                clicked = true,
            )

        assertEquals(NotificationPayload.CollectReason.CLICK, reason)
    }

    @Test
    fun `클릭 수집을 끄면 앱과 키워드가 일치해도 클릭 이벤트에서는 수집하지 않는다`() {
        val reason =
            NotificationFilter(
                collectOnClick = false,
                keywords = setOf("회의"),
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.allowed",
                title = "회의 시작",
                text = null,
                clicked = true,
            )

        assertNull(reason)
    }

    @Test
    fun `클릭 수집을 꺼도 게시 알림의 앱과 키워드 필터는 동작한다`() {
        val reason =
            NotificationFilter(
                collectOnClick = false,
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.allowed",
                title = "일반 알림",
                text = null,
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.APP, reason)
    }

    @Test
    fun `선택 앱의 게시 알림을 수집한다`() {
        val reason =
            NotificationFilter(
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.allowed",
                title = "제목",
                text = "본문",
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.APP, reason)
    }

    @Test
    fun `제목이나 본문의 키워드는 대소문자 구분 없이 수집한다`() {
        val filter = NotificationFilter(keywords = setOf("Laimory"))

        val titleReason =
            filter.collectReasonFor(
                packageName = "com.example",
                title = "LAIMORY 알림",
                text = null,
                clicked = false,
            )
        val textReason =
            filter.collectReasonFor(
                packageName = "com.example",
                title = null,
                text = "오늘 laimory 기록을 확인하세요",
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.KEYWORD, titleReason)
        assertEquals(NotificationPayload.CollectReason.KEYWORD, textReason)
    }

    @Test
    fun `키워드와 앱이 함께 일치하면 키워드를 대표 사유로 사용한다`() {
        val reason =
            NotificationFilter(
                keywords = setOf("회의"),
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.allowed",
                title = "회의 시작",
                text = null,
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.KEYWORD, reason)
    }

    @Test
    fun `빈 키워드와 불일치 앱의 게시 알림은 수집하지 않는다`() {
        val reason =
            NotificationFilter(
                keywords = setOf("", "   "),
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.other",
                title = "일반 알림",
                text = "필터와 일치하지 않음",
                clicked = false,
            )

        assertNull(reason)
    }

    // --- 기본 키워드 ---

    @Test
    fun `기본 키워드로 사용자 설정 없이도 생활 이벤트를 수집한다`() {
        val reason =
            NotificationFilter().collectReasonFor(
                packageName = COMMERCE_APP,
                title = "주문 배송 시작",
                text = null,
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.KEYWORD, reason)
    }

    @Test
    fun `기본 키워드를 끄면 사용자 키워드만으로 판정한다`() {
        val filter = NotificationFilter(useDefaultKeywords = false, keywords = setOf("회의"))

        val defaultKeyword =
            filter.collectReasonFor(packageName = COMMERCE_APP, title = "주문 배송 시작", text = null, clicked = false)
        val userKeyword =
            filter.collectReasonFor(packageName = UNSCOPED_APP, title = "회의 시작", text = null, clicked = false)

        assertNull(defaultKeyword)
        assertEquals(NotificationPayload.CollectReason.KEYWORD, userKeyword)
    }

    @Test
    fun `기본 키워드를 켜도 사용자 키워드는 함께 동작한다`() {
        val filter = NotificationFilter(keywords = setOf("회의"))

        val userKeyword =
            filter.collectReasonFor(packageName = UNSCOPED_APP, title = "회의 시작", text = null, clicked = false)
        val defaultKeyword =
            filter.collectReasonFor(packageName = FINANCE_APP, title = "결제 승인", text = null, clicked = false)

        assertEquals(NotificationPayload.CollectReason.KEYWORD, userKeyword)
        assertEquals(NotificationPayload.CollectReason.KEYWORD, defaultKeyword)
    }

    // --- 비이벤트 제외 ---

    @Test
    fun `진행 중과 진행률 묶음 요약 광고 알림은 수집하지 않는다`() {
        val filter = NotificationFilter(allowedPackages = setOf("com.example"))
        val nonEvents =
            listOf(
                NotificationSignals(isOngoing = true),
                NotificationSignals(hasProgress = true),
                NotificationSignals(isGroupSummary = true),
                NotificationSignals(isPromotion = true),
            )

        nonEvents.forEach { signals ->
            assertNull(
                filter.collectReasonFor(
                    packageName = "com.example",
                    title = "주문 배송 중",
                    text = null,
                    clicked = false,
                    signals = signals,
                ),
            )
        }
    }

    @Test
    fun `클릭한 알림에는 비이벤트 제외를 적용하지 않는다`() {
        val reason =
            NotificationFilter().collectReasonFor(
                packageName = "com.example",
                title = "배달 실시간 추적",
                text = null,
                clicked = true,
                signals = NotificationSignals(isOngoing = true, isPromotion = true),
            )

        assertEquals(NotificationPayload.CollectReason.CLICK, reason)
    }

    @Test
    fun `구조 신호를 알 수 없으면 비이벤트 제외를 적용하지 않는다`() {
        val reason =
            NotificationFilter().collectReasonFor(
                packageName = FINANCE_APP,
                title = "결제 승인",
                text = null,
                clicked = false,
                signals = NotificationSignals.UNAVAILABLE,
            )

        assertEquals(NotificationPayload.CollectReason.KEYWORD, reason)
    }

    // --- 광고 표기 제외 ---

    @Test
    fun `괄호로 감싼 광고 표기가 있으면 키워드가 일치해도 수집하지 않는다`() {
        val filter = NotificationFilter()
        val advertisements =
            listOf(
                "(광고) 오늘만 배송비 무료",
                "[광고] 주문하면 사은품 증정",
                "(동영상 광고) 지금 예약하면 할인",
                "(광고성 정보) 신상품 도착",
            )

        advertisements.forEach { title ->
            assertNull(
                title,
                filter.collectReasonFor(packageName = COMMERCE_APP, title = title, text = null, clicked = false),
            )
        }
    }

    @Test
    fun `광고 표기가 있어도 사용자가 클릭한 알림은 수집한다`() {
        val reason =
            NotificationFilter().collectReasonFor(
                packageName = "com.example",
                title = "(광고) 오늘만 배송비 무료",
                text = null,
                clicked = true,
            )

        assertEquals(NotificationPayload.CollectReason.CLICK, reason)
    }

    @Test
    fun `광고 표기가 있어도 사용자가 고른 앱의 알림은 수집한다`() {
        val reason =
            NotificationFilter(
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.allowed",
                title = "(광고) 오늘만 배송비 무료",
                text = null,
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.APP, reason)
    }

    @Test
    fun `정상 대괄호 접두와 개인정보 마스킹 토큰은 광고 표기로 보지 않는다`() {
        val filter = NotificationFilter()
        val titles =
            listOf(
                "[CJ대한통운] 고객님의 상품이 배송 완료되었습니다",
                "[Web발신] 주문 취소 환불 12,000원",
                "[상세주소]로 배송이 시작되었습니다",
                "[전화번호] 기사님이 픽업했습니다",
            )

        titles.forEach { title ->
            assertEquals(
                title,
                NotificationPayload.CollectReason.KEYWORD,
                filter.collectReasonFor(packageName = COMMERCE_APP, title = title, text = null, clicked = false),
            )
        }
    }

    @Test
    fun `광고를 부정하는 괄호 표기는 제외하지 않는다`() {
        val filter = NotificationFilter()
        val titles =
            listOf(
                "(비광고) 결제 승인",
                "(광고 없음) 결제 승인",
                "결제 승인 (광고 차단)",
                "(광고 제거) 결제 취소",
            )

        titles.forEach { title ->
            assertEquals(
                title,
                NotificationPayload.CollectReason.KEYWORD,
                filter.collectReasonFor(packageName = FINANCE_APP, title = title, text = null, clicked = false),
            )
        }
    }

    @Test
    fun `괄호 밖의 광고라는 낱말만으로는 제외하지 않는다`() {
        val reason =
            NotificationFilter().collectReasonFor(
                packageName = FINANCE_APP,
                title = "광고 없는 요금제 결제가 승인되었습니다",
                text = null,
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.KEYWORD, reason)
    }

    // --- 기본 키워드 앱 범위 (#270) ---

    @Test
    fun `기본 키워드는 도메인 scope 안의 앱에서만 걸린다`() {
        val filter = NotificationFilter()

        val scoped =
            filter.collectReasonFor(packageName = COMMERCE_APP, title = "상품이 도착했습니다", text = null, clicked = false)
        val unscoped =
            filter.collectReasonFor(packageName = UNSCOPED_APP, title = "보상이 도착했습니다", text = null, clicked = false)

        assertEquals(NotificationPayload.CollectReason.KEYWORD, scoped)
        assertNull(unscoped)
    }

    @Test
    fun `다른 도메인의 키워드는 그 도메인 앱에서만 걸린다`() {
        val filter = NotificationFilter()

        // `배송`은 커머스에만 있고 금융에는 없다.
        val commerce =
            filter.collectReasonFor(packageName = COMMERCE_APP, title = "배송 시작", text = null, clicked = false)
        val finance =
            filter.collectReasonFor(packageName = FINANCE_APP, title = "배송 시작", text = null, clicked = false)

        assertEquals(NotificationPayload.CollectReason.KEYWORD, commerce)
        assertNull(finance)
    }

    @Test
    fun `사용자가 기본 키워드와 같은 단어를 등록하면 목록 밖 앱에서도 수집한다`() {
        val filter = NotificationFilter(keywords = setOf("도착"))

        val reason =
            filter.collectReasonFor(packageName = UNSCOPED_APP, title = "보상이 도착했습니다", text = null, clicked = false)

        assertEquals(NotificationPayload.CollectReason.KEYWORD, reason)
    }

    @Test
    fun `기본 키워드를 끄면 scope 안에서도 기본 키워드로 수집하지 않는다`() {
        val filter = NotificationFilter(useDefaultKeywords = false)

        val reason =
            filter.collectReasonFor(packageName = COMMERCE_APP, title = "주문 배송 시작", text = null, clicked = false)

        assertNull(reason)
    }

    @Test
    fun `목록 밖 앱이라도 allowlist 에 있으면 APP 으로 수집한다`() {
        val filter = NotificationFilter(allowedPackages = setOf(UNSCOPED_APP))

        val reason =
            filter.collectReasonFor(packageName = UNSCOPED_APP, title = "보상이 도착했습니다", text = null, clicked = false)

        assertEquals(NotificationPayload.CollectReason.APP, reason)
    }

    @Test
    fun `광고 표기와 scope 키워드가 함께 맞아도 allowlist 앱이면 APP 으로 수집한다`() {
        val filter = NotificationFilter(allowedPackages = setOf(COMMERCE_APP))

        val reason =
            filter.collectReasonFor(packageName = COMMERCE_APP, title = "(광고) 배송비 무료", text = null, clicked = false)

        assertEquals(NotificationPayload.CollectReason.APP, reason)
    }

    @Test
    fun `여러 scope 에 걸친 키워드는 한 scope 만 통과해도 수집한다`() {
        val filter = NotificationFilter()

        // `도착`은 커머스·배달·이동 세 도메인에 있다. 앱은 그중 하나에만 속한다.
        val delivery =
            filter.collectReasonFor(packageName = DELIVERY_APP, title = "도착 예정", text = null, clicked = false)
        val travel =
            filter.collectReasonFor(packageName = TRAVEL_APP, title = "도착 예정", text = null, clicked = false)

        assertEquals(NotificationPayload.CollectReason.KEYWORD, delivery)
        assertEquals(NotificationPayload.CollectReason.KEYWORD, travel)
    }

    @Test
    fun `기본 문자 앱은 도메인이 섞여 오므로 기본 키워드 전체가 걸린다`() {
        val filter = NotificationFilter()
        val samples =
            listOf(
                "[Web발신] KB국민 승인 12,000원",
                "[Web발신] CJ대한통운 배송완료",
                "[Web발신] 서울내과 예약 확인 8/21 14:30",
                "[Web발신] 대한항공 탑승 수속 안내",
            )

        samples.forEach { text ->
            assertEquals(
                text,
                NotificationPayload.CollectReason.KEYWORD,
                filter.collectReasonFor(packageName = MESSAGING_APP, title = null, text = text, clicked = false),
            )
        }
    }

    @Test
    fun `클릭은 앱 범위 게이트를 우회한다`() {
        val reason =
            NotificationFilter().collectReasonFor(
                packageName = UNSCOPED_APP,
                title = "보상이 도착했습니다",
                text = null,
                clicked = true,
            )

        assertEquals(NotificationPayload.CollectReason.CLICK, reason)
    }
}

/** 금융·결제 scope 에 든 앱. */
private const val FINANCE_APP = "com.kakaobank.channel"

/** 커머스·택배 scope 에 든 앱. */
private const val COMMERCE_APP = "com.coupang.mobile"

/** 배달 scope 에 든 앱. */
private const val DELIVERY_APP = "com.sampleapp"

/** 이동·여행 scope 에 든 앱. */
private const val TRAVEL_APP = "com.korail.talk"

/** 기본 문자 앱. 도메인이 섞여 오므로 기본 키워드 전체가 걸린다. */
private const val MESSAGING_APP = "com.samsung.android.messaging"

/** 어느 scope 에도 없는 앱. 기본 키워드가 걸리면 안 된다. */
private const val UNSCOPED_APP = "com.example.game"
