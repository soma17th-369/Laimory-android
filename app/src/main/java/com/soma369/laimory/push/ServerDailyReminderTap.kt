package com.soma369.laimory.push

import android.content.Intent

/**
 * 서버가 보낸 일일 리마인더 FCM 을 눌러 들어온 것인지.
 *
 * 앱이 꺼져 있거나 백그라운드일 때 시스템이 띄운 FCM 알림은, 누르면 메시지의 data 를 그대로 extra 로 실어 MainActivity 를 연다.
 * 앱은 이 `route` 로 화면을 고르지 않는다 — 로컬 리마인더로 옮긴 뒤로 서버는 대개 보내지 않고, 와도 홈으로 연다. 분석에서
 * 로컬 리마인더와 나눠 세려고만 읽는다.
 */
object ServerDailyReminderTap {
    private const val ROUTE_KEY = "route"
    private const val ROUTE_TODAY_TIMELINE = "timeline/today"

    /** 서버 리마인더로 들어온 것이면 `true` 를 돌려주고 extra 를 지운다 — 재생성 때 다시 세지 않게. */
    fun consume(intent: Intent): Boolean {
        if (intent.getStringExtra(ROUTE_KEY) != ROUTE_TODAY_TIMELINE) return false
        intent.removeExtra(ROUTE_KEY)
        return true
    }
}
