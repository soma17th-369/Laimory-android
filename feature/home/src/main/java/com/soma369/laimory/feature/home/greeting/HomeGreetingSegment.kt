package com.soma369.laimory.feature.home.greeting

/** 한 문장을 이루는 인사말 조각. 화면이 [emphasis]를 색으로 옮긴다. */
data class HomeGreetingSegment(
    val text: String,
    val emphasis: HomeGreetingEmphasis,
)
