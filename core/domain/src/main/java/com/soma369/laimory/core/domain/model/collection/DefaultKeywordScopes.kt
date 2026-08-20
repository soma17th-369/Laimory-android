package com.soma369.laimory.core.domain.model.collection

/**
 * 앱이 내장하는 기본 키워드와 그 키워드가 걸릴 앱 범위.
 *
 * 기본 키워드를 전 앱에 걸면 게임 푸시(`보상 도착`), OTT(`새 에피소드 도착`), 뉴스 헤드라인이
 * 그대로 잡힌다. 도메인 단위로 앱을 좁혀 생활 이벤트만 남긴다.
 *
 * 사용자가 직접 등록한 키워드에는 이 범위를 적용하지 않는다 — 사용자가 넣은 키워드는 의도가
 * 담긴 것이라 목록 밖 앱에서 조용히 무시되면 안 된다.
 *
 * ## application ID 검증
 *
 * 아래 ID 는 아직 **검증 전 후보**다. 틀린 ID 는 예외가 아니라 침묵으로 실패한다 — 해당 앱의
 * 알림이 조용히 0건이 되고 테스트로도 잡히지 않는다. 배포 전에 두 가지로 확정한다.
 *
 * 1. 실기기 `adb shell pm list packages` — 실제 설치된 앱의 ID
 * 2. Google Play URL 의 `id` 파라미터 — 공식 application ID
 *
 * 확인되지 않은 항목은 추측으로 남기지 않고 뺀다.
 */
val DEFAULT_KEYWORD_SCOPES: List<KeywordScope> =
    listOf(
        KeywordScope(
            keywords = setOf("결제", "승인", "환불", "취소"),
            apps =
                setOf(
                    // KB스타뱅킹
                    AppMatch.Exact("com.kbstar.kbbank"),
                    // KB Pay
                    AppMatch.Exact("com.kbcard.cxh.appcard"),
                    // 신한 SOL뱅크
                    AppMatch.Exact("com.shinhan.sbanking"),
                    // 신한 SOL페이
                    AppMatch.Exact("com.shcard.smartpay"),
                    // 우리WON뱅킹
                    AppMatch.Exact("com.wooribank.smart.npib"),
                    // 하나원큐
                    AppMatch.Exact("com.kebhana.hanapush"),
                    // NH스마트뱅킹
                    AppMatch.Exact("nh.smart.banking"),
                    // 카카오뱅크
                    AppMatch.Exact("com.kakaobank.channel"),
                    // 토스
                    AppMatch.Exact("viva.republica.toss"),
                    // 삼성월렛
                    AppMatch.Exact("com.samsung.android.spay"),
                    // 현대카드
                    AppMatch.Exact("com.hyundaicard.appcard"),
                    // 페이코
                    AppMatch.Exact("com.nhnent.payapp"),
                ),
        ),
        KeywordScope(
            keywords = setOf("주문", "배송", "픽업", "도착", "환불", "취소"),
            apps =
                setOf(
                    // 쿠팡
                    AppMatch.Exact("com.coupang.mobile"),
                    // 11번가
                    AppMatch.Exact("com.elevenst"),
                    // G마켓
                    AppMatch.Exact("com.ebay.kr.gmarket"),
                    // 옥션
                    AppMatch.Exact("com.ebay.kr.auction"),
                    // SSG닷컴
                    AppMatch.Exact("com.ssg.emart.app"),
                    // 롯데온
                    AppMatch.Exact("com.lotte.on"),
                    // CJ대한통운
                    AppMatch.Exact("com.cjkoreaexpress"),
                ),
        ),
        KeywordScope(
            keywords = setOf("배달", "주문", "픽업", "도착", "취소"),
            apps =
                setOf(
                    // 배달의민족
                    AppMatch.Exact("com.sampleapp"),
                    // 요기요
                    AppMatch.Exact("com.fineapp.yogiyo"),
                    // 쿠팡이츠
                    AppMatch.Exact("com.coupang.mobile.eats"),
                ),
        ),
        KeywordScope(
            keywords = setOf("예약", "예매", "환불", "취소"),
            apps =
                setOf(
                    // CGV
                    AppMatch.Exact("com.cgv.android.movieapp"),
                    // 메가박스
                    AppMatch.Exact("com.megabox.mop"),
                    // 인터파크 티켓
                    AppMatch.Exact("com.interpark.app.ticket"),
                    // 야놀자
                    AppMatch.Exact("com.yanolja.repo"),
                    // 여기어때
                    AppMatch.Exact("com.gccompany.dayuse"),
                ),
        ),
        KeywordScope(
            keywords = setOf("출발", "도착", "탑승", "예약", "환불", "취소"),
            apps =
                setOf(
                    // 대한항공
                    AppMatch.Exact("com.koreanair.passenger"),
                    // 아시아나항공
                    AppMatch.Exact("com.flyasiana.mobile"),
                    // 코레일톡
                    AppMatch.Exact("com.korail.talk"),
                    // T맵
                    AppMatch.Exact("com.skt.tmap.ku"),
                    // 카카오 T
                    AppMatch.Exact("com.kakao.taxi"),
                ),
        ),
    )
