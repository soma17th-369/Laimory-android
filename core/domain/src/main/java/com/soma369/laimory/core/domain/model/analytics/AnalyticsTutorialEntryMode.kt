package com.soma369.laimory.core.domain.model.analytics

/** 튜토리얼이 어떻게 시작됐는지. */
enum class AnalyticsTutorialEntryMode {
    /** 봤다는 기록이 없어 저절로 떴다. debug 초기화 뒤 다시 뜬 것도 여기다. */
    FIRST_RUN,

    /** 설정 `사용 방법` 에서 골라 다시 봤다. */
    REPLAY,
}
