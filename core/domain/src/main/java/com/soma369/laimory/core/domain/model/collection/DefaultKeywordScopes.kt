package com.soma369.laimory.core.domain.model.collection

/** 기본 키워드 전체. 도메인 scope 들이 이 집합을 모두 덮는지 테스트로 고정한다. */
internal val ALL_DEFAULT_KEYWORDS: Set<String> =
    setOf(
        "결제", "승인", "환불",
        "주문", "배송", "배달", "픽업",
        "예약", "예매",
        "출발", "도착", "탑승",
        "취소",
    )

/**
 * 기본 문자 앱 scope.
 *
 * 결제 승인·택배·예약 확인은 앱 푸시가 아니라 문자로 오는 비중이 크다. 카드사 앱을 쓰지 않는
 * 사용자는 승인 알림이 전부 문자고, 병원·미용실·식당 예약 확인은 앱 자체가 없어 문자뿐이다.
 *
 * 문자에는 도메인이 섞여 오므로 기본 키워드 전체를 건다. 도메인별로 나눌 근거가 없다.
 *
 * 대화 알림을 통과시키는 판정은 여기가 아니라 [NotificationPrivacyPolicy] 가 한다 —
 * 기업 발송 표기가 없는 문자는 이 scope 에 닿기 전에 걸러진다.
 */
val MESSAGING_SCOPE: KeywordScope =
    KeywordScope(
        keywords = ALL_DEFAULT_KEYWORDS,
        apps =
            setOf(
                // 삼성 메시지
                AppMatch.Exact("com.samsung.android.messaging"),
                // 구글 메시지
                AppMatch.Exact("com.google.android.apps.messaging"),
            ),
    )

/** 앱 종류로 나뉘는 도메인 scope. 문자 앱은 [MESSAGING_SCOPE] 로 따로 둔다. */
internal val DOMAIN_SCOPES: List<KeywordScope> =
    listOf(
        KeywordScope(
            keywords = setOf("결제", "승인", "환불", "취소"),
            apps =
                setOf(
                    // KB스타뱅킹
                    AppMatch.Exact("com.kbstar.kbbank"),
                    // KB Pay
                    AppMatch.Exact("com.kbcard.cxh.appcard"),
                    // 신한 슈퍼SOL
                    AppMatch.Exact("com.shinhan.sbanking"),
                    // 신한 SOL페이
                    AppMatch.Exact("com.shcard.smartpay"),
                    // 우리은행 우리WON뱅킹
                    AppMatch.Exact("com.wooribank.smart.npib"),
                    // 우리카드 우리WON카드
                    AppMatch.Exact("com.wooricard.smartapp"),
                    // 하나원큐
                    AppMatch.Exact("com.hanabank.oqf"),
                    // (구)하나원큐 — 종료 예정이나 전환기 사용자를 위해 남긴다
                    AppMatch.Exact("com.kebhana.hanapush"),
                    // 하나Pay
                    AppMatch.Exact("com.hanaskcard.paycla"),
                    // NH스마트뱅킹
                    AppMatch.Exact("nh.smart.banking"),
                    // i-ONE Bank
                    AppMatch.Exact("com.ibk.android.ionebank"),
                    // iM뱅크
                    AppMatch.Exact("kr.co.dgb.dgbm"),
                    // BNK부산은행
                    AppMatch.Exact("kr.co.busanbank.mbp"),
                    // MG더뱅킹
                    AppMatch.Exact("com.smg.spbs"),
                    // 신협 온뱅크
                    AppMatch.Exact("kr.co.cu.onbank"),
                    // 우체국뱅킹
                    AppMatch.Exact("com.epost.psf.sdsi"),
                    // 카카오뱅크
                    AppMatch.Exact("com.kakaobank.channel"),
                    // 케이뱅크
                    AppMatch.Exact("com.kbankwith.smartbank"),
                    // 토스
                    AppMatch.Exact("viva.republica.toss"),
                    // 카카오페이
                    AppMatch.Exact("com.kakaopay.app"),
                    // 네이버페이
                    AppMatch.Exact("com.naverfin.payapp"),
                    // 페이코
                    AppMatch.Exact("com.nhnent.payapp"),
                    // 페이북/ISP
                    AppMatch.Exact("kvp.jjy.MispAndroid320"),
                    // 삼성 월렛
                    AppMatch.Exact("com.samsung.android.spay"),
                    // 현대카드
                    AppMatch.Exact("com.hyundaicard.appcard"),
                    // 디지로카(롯데카드)
                    AppMatch.Exact("com.lcacApp"),
                    // 모니모(삼성금융)
                    AppMatch.Exact("net.ib.android.smcard"),
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
                    // SSG.COM
                    AppMatch.Exact("kr.co.ssg"),
                    // 롯데ON
                    AppMatch.Exact("com.lotte"),
                    // 네이버
                    AppMatch.Exact("com.nhn.android.search"),
                    // 컬리
                    AppMatch.Exact("com.dbs.kurly.m2"),
                    // 올리브영
                    AppMatch.Exact("com.oliveyoung"),
                    // 홈플러스
                    AppMatch.Exact("com.socialapps.homeplus"),
                    // 이마트
                    AppMatch.Exact("com.emart.today"),
                    // 다이소몰
                    AppMatch.Exact("com.uxlayer.wipoint"),
                    // 무신사
                    AppMatch.Exact("com.musinsa.store"),
                    // 오늘의집
                    AppMatch.Exact("net.bucketplace"),
                    // 에이블리
                    AppMatch.Exact("com.banhala.android"),
                    // 지그재그
                    AppMatch.Exact("com.croquis.zigzag"),
                    // 29CM
                    AppMatch.Exact("com.the29cm.app29cm"),
                    // CJ대한통운 오네
                    AppMatch.Exact("com.cjkoreaexpress"),
                    // 한진택배
                    AppMatch.Exact("com.hanjintransportation.oneclick"),
                    // 롯데택배
                    AppMatch.Exact("com.glogis.lemp.malcs"),
                    // 로젠택배
                    AppMatch.Exact("com.ilogen.delivery"),
                    // 우체국
                    AppMatch.Exact("kr.go.epost.app.findZip"),
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
                    // 땡겨요
                    AppMatch.Exact("com.shinhan.o2o"),
                    // 스타벅스
                    AppMatch.Exact("com.starbucks.co"),
                    // 메가MGC커피
                    AppMatch.Exact("co.kr.waldlust.megacoffee"),
                    // 컴포즈커피
                    AppMatch.Exact("ci.dvn.composecoffee.app"),
                    // 이디야멤버스
                    AppMatch.Exact("com.ediya.coupon"),
                    // 투썸하트
                    AppMatch.Exact("com.cj.twosome"),
                    // 맥도날드
                    AppMatch.Exact("com.mcdonalds.mobileapp"),
                    // 버거킹
                    AppMatch.Exact("kr.co.burgerkinghybrid"),
                ),
        ),
        KeywordScope(
            keywords = setOf("예약", "예매", "환불", "취소"),
            apps =
                setOf(
                    // CGV
                    AppMatch.Exact("co.kr.cgv.cjcgv"),
                    // 메가박스
                    AppMatch.Exact("com.megabox.mop"),
                    // 롯데시네마
                    AppMatch.Exact("kr.co.lottecinema.lcm"),
                    // NOL 티켓(구 인터파크 티켓)
                    AppMatch.Exact("com.interpark.app.ticket"),
                    // 예스24 티켓
                    AppMatch.Exact("com.yes24.ticket"),
                    // 멜론티켓
                    AppMatch.Exact("com.iloen.melonticket"),
                    // NOL(야놀자)
                    AppMatch.Exact("com.cultsotry.yanolja.nativeapp"),
                    // 여기어때
                    AppMatch.Exact("kr.goodchoice.abouthere"),
                    // 에어비앤비
                    AppMatch.Exact("com.airbnb.android"),
                    // 아고다
                    AppMatch.Exact("com.agoda.mobile.consumer"),
                    // Booking.com
                    AppMatch.Exact("com.booking"),
                    // 캐치테이블
                    AppMatch.Exact("co.kr.catchtable.android.catchtable_app"),
                    // 테이블링
                    AppMatch.Exact("com.mealant.tabling"),
                    // 똑닥
                    AppMatch.Exact("com.bbros.sayup"),
                    // 네이버
                    AppMatch.Exact("com.nhn.android.search"),
                ),
        ),
        KeywordScope(
            keywords = setOf("출발", "도착", "탑승", "예약", "환불", "취소"),
            apps =
                setOf(
                    // 대한항공 My
                    AppMatch.Exact("com.koreanair.passenger"),
                    // 아시아나항공
                    AppMatch.Exact("com.ssm.asiana"),
                    // 제주항공
                    AppMatch.Exact("com.parksmt.jejuair.android16"),
                    // 진에어
                    AppMatch.Exact("com.jinair.android"),
                    // 티웨이항공
                    AppMatch.Exact("com.twayair.m.app"),
                    // 에어부산
                    AppMatch.Exact("com.airbusan.gcm"),
                    // 코레일+
                    AppMatch.Exact("com.korail.talk"),
                    // SRT
                    AppMatch.Exact("kr.co.srail.newapp"),
                    // 티맵
                    AppMatch.Exact("com.skt.tmap.ku"),
                    // 카카오 T
                    AppMatch.Exact("com.kakao.taxi"),
                    // 쏘카
                    AppMatch.Exact("socar.Socar"),
                    // 롯데렌터카 G car(구 그린카)
                    AppMatch.Exact("com.greencar"),
                    // 마이리얼트립
                    AppMatch.Exact("com.mrt.ducati"),
                    // 스카이스캐너
                    AppMatch.Exact("net.skyscanner.android.main"),
                ),
        ),
    )

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
 * 아래 ID 는 두 가지로 확정했다. 틀린 ID 는 예외가 아니라 침묵으로 실패한다 — 해당 앱의 알림이
 * 조용히 0건이 되고 테스트로도 잡히지 않는다.
 *
 * 1. 실기기 `adb shell pm list packages` — 실제 설치된 앱의 ID
 * 2. Google Play 상세 페이지 — application ID 와 앱 이름이 함께 맞는지
 *
 * 항목을 더할 때도 같은 절차를 지킨다. 확인되지 않은 항목은 추측으로 남기지 않는다.
 * 주석의 앱 이름은 Play 등재명을 따른다 — 리브랜딩된 앱을 알아보기 위해서다.
 */
val DEFAULT_KEYWORD_SCOPES: List<KeywordScope> = DOMAIN_SCOPES + MESSAGING_SCOPE
