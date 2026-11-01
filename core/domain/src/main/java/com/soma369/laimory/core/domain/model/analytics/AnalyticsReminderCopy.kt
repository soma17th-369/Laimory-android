package com.soma369.laimory.core.domain.model.analytics

/**
 * 로컬 리마인더가 띄운 문구.
 *
 * [title] 은 알림에 보인 제목 그대로다. 앱이 쓴 문구라 사용자 원문이 아니어서 싣는다 — 배열 순서(index)를 실으면
 * 문구를 추가 · 삭제 · 재배열할 때 옛 데이터와 섞인다.
 */
data class AnalyticsReminderCopy(
    val kind: AnalyticsReminderKind,
    val title: String,
)
