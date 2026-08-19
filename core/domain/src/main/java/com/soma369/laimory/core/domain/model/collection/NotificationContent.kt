package com.soma369.laimory.core.domain.model.collection

/**
 * 알림 이벤트당 한 번만 추출한 제목·본문.
 *
 * 개인정보 판정([NotificationPrivacyPolicy]), 수집 판정([NotificationFilter]), 로컬 저장이
 * 모두 이 값을 공유한다 — 각 단계가 알림 객체에서 따로 텍스트를 읽으면 정제 전 원문이
 * 저장 경로로 새기 때문이다.
 */
data class NotificationContent(
    val title: String?,
    val text: String?,
)
