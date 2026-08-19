package com.soma369.laimory.feature.home.greeting

/** 인사말 조각의 강조 수준. 굵기가 아니라 색 대비로 표현한다(Figma 규격). */
enum class HomeGreetingEmphasis {
    /** `안녕하세요, `·`님` 같은 문장 골격. */
    NORMAL,

    /** 닉네임 구간. */
    NICKNAME,
}
