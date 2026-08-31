package com.soma369.laimory.feature.onboarding.model

import com.soma369.laimory.core.ui.permission.DataPermission

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
            key = "photo",
            label = "PHOTO",
            title = "사진에 시간과 장소가\n남아 있어요",
            description =
                "찍은 사진에서 시각과 위치만 읽어 하루의 순간을 채워요.\n" +
                    "전체가 부담스러우면 고른 사진만 허용해도 돼요.",
            permission = DataPermission.PHOTO,
            primaryCta = "사진 연결하기",
        ),
        OnboardingPageSpec(
            key = "calendar",
            label = "CALENDAR",
            title = "쓰던 캘린더를\n그대로 읽어요",
            description =
                "이미 적어 둔 일정을 그대로 가져와 하루의 뼈대를 세워요.\n" +
                    "읽기만 하고 일정을 바꾸지 않아요.",
            permission = DataPermission.CALENDAR,
            primaryCta = "캘린더 연결하기",
        ),
        OnboardingPageSpec(
            key = "location",
            label = "PLACE",
            title = "오간 길이\n하루의 뼈대가 돼요",
            description =
                "머문 곳과 이동한 구간만 남겨 하루의 흐름을 세워요.\n" +
                    "주머니에 넣고 다닌 구간까지 이으려면 '항상 허용'이 필요해요.",
            permission = DataPermission.LOCATION,
            primaryCta = "위치 연결하기",
        ),
        OnboardingPageSpec(
            key = "notification",
            label = "NOTIFICATION",
            title = "지나간 알림에도\n하루가 있어요",
            description =
                "결제·배송·예약처럼 생활 이벤트를 알리는 알림만 읽어요.\n" +
                    "개인 대화와 광고 알림은 수집하지 않아요.",
            permission = DataPermission.NOTIFICATION_LISTENER,
            primaryCta = "알림 접근 켜기",
        ),
        OnboardingPageSpec(
            key = "app_notification",
            label = "REMINDER",
            title = "하루가 정리되면\n알려드릴게요",
            description = "타임라인이 완성됐을 때와 기록을 남길 시간에만 알려요.",
            permission = DataPermission.APP_NOTIFICATION,
            primaryCta = "알림 받기",
        ),
        // 필수 동의를 마지막 장에 둔다. 소스별로 무엇을 읽어 무엇에 쓰는지 다 읽은 뒤라야
        // 무엇을 보내는지 알고 판단할 수 있다 — 첫 장에 두면 아무것도 연결하지 않은 상태에서
        // 민감정보·제3자 제공·국외 이전·위치를 묻게 된다.
        OnboardingPageSpec(
            key = "done",
            title = "준비됐어요",
            description = "지금 켜 두지 않은 것도 설정에서 언제든 바꿀 수 있어요.",
            showsConsents = true,
            primaryCta = "Laimory 시작하기",
        ),
    )

/** 마지막으로 본 장으로 되돌린다. 모르는 키는 첫 장이다 — 목록이 바뀌어 사라진 장일 수 있다. */
fun List<OnboardingPageSpec>.indexOfKeyOrFirst(key: String?): Int = indexOfFirst { it.key == key }.takeIf { it >= 0 } ?: 0
