package com.soma369.laimory.feature.onboarding.model

import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.feature.onboarding.R

/**
 * 온보딩에 실제로 띄우는 장의 순서.
 *
 * 순서를 바꾸거나 장을 더하는 일은 이 목록만 고친다. 문구는 Figma 소개 스토리보드를 정본으로 쓴다.
 *
 * 제목의 줄바꿈은 폭에 맡긴다 — 글자 크기를 키운 사용자나 좁은 기기에서 손으로 넣은 자리가
 * 오히려 어긋난다. 첫 장만 예외로 손으로 나눈다. `오늘 하루,` 에서 끊어야 문장이 읽히는데,
 * 폭에 맡기면 `오늘 하루, AI가` 로 붙어 의미가 갈리는 자리에서 끊기지 않는다.
 */
val ONBOARDING_PAGES: List<OnboardingPageSpec> =
    listOf(
        OnboardingPageSpec(
            key = "intro",
            // 첫 장은 설명 대신 타임라인 예시가 무엇을 만드는지 보여 준다.
            title = "오늘 하루,\nAI가 한눈에 정리해드려요.",
            image = R.drawable.img_onboarding_intro_timeline,
            scrollsImage = true,
            primaryCta = "시작하기",
            showsGreeting = true,
        ),
        OnboardingPageSpec(
            key = "photo",
            label = "PHOTO",
            title = "찍은 사진이 하루의 순간이 돼요",
            description = "언제 어디서 찍었는지에 더해, 사진에 어떤 순간이 담겼는지까지 AI가 읽어 타임라인을 채워요.",
            image = R.drawable.img_onboarding_photo,
            permission = DataPermission.PHOTO,
            primaryCta = "사진 연결하기",
        ),
        OnboardingPageSpec(
            key = "calendar",
            label = "CALENDAR",
            title = "적어 둔 일정이 하루의 큰 줄기가 돼요",
            description = "캘린더의 일정을 바탕으로 하루의 틀을 먼저 잡고, 그 위에 나머지 기록을 채워 나가요.",
            image = R.drawable.img_onboarding_calendar,
            permission = DataPermission.CALENDAR,
            primaryCta = "캘린더 연결하기",
        ),
        OnboardingPageSpec(
            key = "location",
            label = "PLACE",
            title = "머문 곳과 오간 길이 하루의 흐름이 돼요",
            description = "위치 기록을 분석해 머문 장소와 이동 구간을 구분하고, 순간과 순간 사이를 자연스럽게 이어요.",
            image = R.drawable.img_onboarding_location,
            permission = DataPermission.LOCATION,
            primaryCta = "위치 연결하기",
        ),
        OnboardingPageSpec(
            key = "notification",
            label = "NOTIFICATION",
            title = "무심코 넘긴 알림도 하루의 조각이 돼요",
            description = "결제·배송·예약 같은 생활 알림을 분석해, 사진도 일정도 남지 않은 순간을 타임라인에 채워요.",
            image = R.drawable.img_onboarding_notification,
            permission = DataPermission.NOTIFICATION_LISTENER,
            primaryCta = "알림 접근 켜기",
        ),
        OnboardingPageSpec(
            key = "app_notification",
            label = "REMINDER",
            title = "하루가 정리되면 알려드릴게요",
            description = "타임라인이 완성됐을 때와 하루를 돌아볼 시간에 맞춰 알려드려요.",
            image = R.drawable.img_onboarding_reminder,
            permission = DataPermission.APP_NOTIFICATION,
            primaryCta = "알림 받기",
        ),
        // 필수 동의를 마지막 장에 둔다. 소스별로 무엇을 읽어 무엇에 쓰는지 다 읽은 뒤라야
        // 무엇을 보내는지 알고 판단할 수 있다 — 첫 장에 두면 아무것도 연결하지 않은 상태에서
        // 민감정보·제3자 제공·국외 이전·위치를 묻게 된다.
        OnboardingPageSpec(
            key = "done",
            brandLabel = "LAIMORY",
            title = "준비됐어요",
            description = "연결한 만큼 타임라인이 풍성해져요. 나머지는 설정에서 언제든 켤 수 있어요.",
            showsConsents = true,
            primaryCta = "Laimory 시작하기",
        ),
    )

/** 마지막으로 본 장으로 되돌린다. 모르는 키는 첫 장이다 — 목록이 바뀌어 사라진 장일 수 있다. */
fun List<OnboardingPageSpec>.indexOfKeyOrFirst(key: String?): Int = indexOfFirst { it.key == key }.takeIf { it >= 0 } ?: 0
