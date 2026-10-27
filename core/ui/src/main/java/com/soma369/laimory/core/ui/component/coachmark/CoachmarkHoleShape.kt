package com.soma369.laimory.core.ui.component.coachmark

/** 코치마크 구멍의 모양. 짚는 자리의 모양을 따른다 — 둥근 버튼을 네모로 뚫으면 자리가 어긋나 보인다. */
enum class CoachmarkHoleShape {
    /** 카드 · 줄 · 버튼. 모서리가 둥근 사각형. */
    ROUNDED_RECT,

    /** 아이콘 버튼 · 떠 있는 버튼. 자리를 둘러싸는 원. */
    CIRCLE,
}
