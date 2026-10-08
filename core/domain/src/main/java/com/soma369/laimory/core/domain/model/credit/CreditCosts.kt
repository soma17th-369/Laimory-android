package com.soma369.laimory.core.domain.model.credit

/**
 * 크레딧을 쓰는 작업별 비용. 서버 상수가 정본이라 앱은 받아서 보여 주기만 한다 — 비용이 바뀌어도 앱 업데이트 없이
 * 따라간다.
 *
 * @property timelineCreation 타임라인 한 번 만들기(AI 결과가 저장될 때 차감).
 */
data class CreditCosts(
    val timelineCreation: Int,
)
