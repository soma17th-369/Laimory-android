package com.soma369.laimory.feature.onboarding.model

/**
 * 온보딩에 실제로 띄우는 장의 순서.
 *
 * 순서를 바꾸거나 장을 더하는 일은 이 목록만 고친다. 문구는 Figma 소개 스토리보드를 정본으로 쓴다.
 */
val ONBOARDING_PAGES: List<OnboardingPageSpec> =
    listOf(
        OnboardingPageSpec(
            key = "intro",
            title = "오늘 뭐 했는지,\nAI가 한눈에 정리해드려요.",
            description =
                "캘린더·사진·위치를 연결해 오늘의 타임라인을 만들어요.\n" +
                    "무엇을 연결할지는 다음 화면에서 하나씩 고르실 수 있어요.",
            primaryCta = "시작하기",
            showsGreeting = true,
        ),
        OnboardingPageSpec(
            key = "done",
            title = "준비됐어요",
            description = "지금 켜 두지 않은 것도 설정에서 언제든 바꿀 수 있어요.",
            primaryCta = "Laimory 시작하기",
        ),
    )

/** 마지막으로 본 장으로 되돌린다. 모르는 키는 첫 장이다 — 목록이 바뀌어 사라진 장일 수 있다. */
fun List<OnboardingPageSpec>.indexOfKeyOrFirst(key: String?): Int = indexOfFirst { it.key == key }.takeIf { it >= 0 } ?: 0
