package com.soma369.laimory.core.domain.helper

import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKey
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent

/**
 * 사용자가 어떤 기능을 이용했고 전환했는지 측정하는 **제품 분석** 포트.
 *
 * 화면은 SDK 를 직접 부르지 않고 이 포트로만 기록한다. 이벤트 이름과 속성이 호출부마다 흩어지면
 * 대시보드와의 계약이 깨지고, 사용자 원문이 섞여 나가도 막을 자리가 없다. 보낼 수 있는 것은
 * [AnalyticsEvent] 로 정의된 것뿐이다.
 *
 * **크래시 로깅([com.soma369.laimory.core.util.logging.Logger])과 합치지 않는다.** 크래시 쪽은
 * 리시버·서비스·워커처럼 주입이 없는 곳에서도 불려야 해 전역이고, 자유 문장을 실패 지점에서 남긴다.
 * 이쪽은 성공이 확정된 지점에서 정해진 이벤트만 보내고 ViewModel 에 주입해 테스트로 검증한다.
 *
 * 전송 대상(버킷)은 구현이 정한다 — 지금은 Firebase(GA4)지만 스프레드시트나 자체 서버로 옮기거나
 * 둘을 병행할 수 있다. 이 포트와 이벤트 정의는 그대로 둔 채 구현만 바꾼다.
 */
interface AnalyticsHelper {
    /** 이벤트를 기록한다. 전송 실패는 앱 동작에 영향을 주지 않는다. */
    suspend fun log(event: AnalyticsEvent)

    /**
     * 같은 [key] 로는 한 번만 기록한다.
     *
     * 앱 재실행·푸시와 폴링의 중복 관찰처럼 같은 논리 사건이 여러 번 관찰되는 이벤트에 쓴다.
     * 판정 기록은 기기에만 남고 [key] 자체는 전송하지 않는다.
     */
    suspend fun logOnce(
        key: AnalyticsDedupeKey,
        event: AnalyticsEvent,
    )

    /**
     * 이후 이벤트를 [userId] 회원으로 묶는다. null 이면 묶음을 푼다.
     *
     * 재설치·기기 변경을 한 사람으로 모으고, 한 기기에서 계정을 바꾸면 사람을 나누기 위한 자리다.
     * 이벤트 속성에는 싣지 않는다 — 버킷의 사용자 구분 자리에만 건다.
     */
    fun setUserId(userId: Long?)
}
