package com.soma369.laimory.core.domain.model.analytics

/**
 * 같은 논리 사건을 한 번만 기록하기 위한 판정 키.
 *
 * 기기에만 저장하고 전송하지 않는다. 계정이 바뀌면 섞이지 않도록 사용자 구분을 포함해 만든다 —
 * 구체적인 키 조립은 이벤트를 심는 후속 이슈가 정한다.
 *
 * [installScoped] 는 "이 설치에서 한 번" 인 키다. 판정 기록은 자동 백업에 실려 새 기기·재설치로
 * 되살아날 수 있어서, 키를 그대로 두면 옛 설치의 판정이 새 설치의 첫 사건을 막는다. 저장할 때 백업에서
 * 제외된 설치 구분 값을 붙여 설치마다 다른 키가 되게 하며, 붙이는 일은 구현([AnalyticsHelper])이 한다.
 */
data class AnalyticsDedupeKey(
    val value: String,
    val installScoped: Boolean = false,
)
