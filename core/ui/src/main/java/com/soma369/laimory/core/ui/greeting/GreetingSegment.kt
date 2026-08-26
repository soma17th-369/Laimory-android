package com.soma369.laimory.core.ui.greeting

/** 한 문장을 이루는 인사말 조각. 화면이 [emphasis]를 색으로 옮긴다. */
data class GreetingSegment(
    val text: String,
    val emphasis: GreetingEmphasis,
)
