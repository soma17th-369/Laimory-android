package com.soma369.laimory.core.domain.model.analytics

/** 알림을 띄운 주체. 서버가 보낸 FCM 인지, 앱이 기기에서 직접 띄운 로컬 알림인지. */
enum class AnalyticsPushDelivery {
    FCM,
    LOCAL,
}
