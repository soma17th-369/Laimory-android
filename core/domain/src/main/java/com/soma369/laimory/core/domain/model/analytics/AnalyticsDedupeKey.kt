package com.soma369.laimory.core.domain.model.analytics

/**
 * 같은 논리 사건을 한 번만 기록하기 위한 판정 키.
 *
 * 기기에만 저장하고 전송하지 않는다. 계정이 바뀌면 섞이지 않도록 사용자 구분을 포함해 만든다 —
 * 구체적인 키 조립은 이벤트를 심는 후속 이슈가 정한다.
 */
@JvmInline
value class AnalyticsDedupeKey(
    val value: String,
)
