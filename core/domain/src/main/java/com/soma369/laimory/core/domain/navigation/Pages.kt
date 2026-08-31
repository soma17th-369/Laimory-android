package com.soma369.laimory.core.domain.navigation

import java.time.LocalDate

/**
 * 앱의 화면 목적지 카탈로그.
 *
 * core:domain 에 두어 feature 간 직접 의존 없이 서로를 목적지로 지목한다
 * (feature A 가 feature B 화면으로 이동할 때 `Page` 만 알면 된다).
 * 인자가 생기면 `data class ... : Page` 로 바꾸고 [Page.toRoute] 에서 args 를 채운다.
 */
data object HomePage : Page {
    const val PATH = "/home"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

data object LoginPage : Page {
    const val PATH = "/login"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/**
 * 이용약관 동의.
 *
 * 서버가 인증 API 대부분을 이용약관 동의 여부로 막으므로 **온보딩보다 앞선 앱 루트**다.
 * 온보딩과 층위는 같지만 상태를 섞지 않는다 — 온보딩은 설치 단위이고 약관 동의는 계정 단위라,
 * 이미 온보딩을 마친 계정도 미동의면 여기부터 지나야 한다.
 */
data object TermsPage : Page {
    const val PATH = "/terms"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/**
 * 로그인 직후 한 번 보여 주는 데이터 권한 온보딩.
 *
 * Login·Home 과 같은 층위의 **앱 루트**다. 밀어 넣는 화면이 아니라, 인증과 온보딩 완료 여부로
 * 셋 중 하나가 정해진다.
 */
data object OnboardingPage : Page {
    const val PATH = "/onboarding"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

data object Feature1Page : Page {
    const val PATH = "/feature1"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

data class TimelinePage(
    val recordDate: LocalDate,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            path = PATH,
            args = mapOf(RECORD_DATE_ARG to recordDate.toString()),
        )

    companion object {
        const val PATH = "/timeline"
        const val RECORD_DATE_ARG = "recordDate"

        fun recordDateFrom(args: Map<String, String>): LocalDate? =
            args[RECORD_DATE_ARG]?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
    }
}

/**
 * 하루 기록에 새 이벤트를 만드는 화면.
 *
 * 편집과 같은 화면을 쓰지만 경로를 나눈다 — 인자가 다르고(기존은 이벤트 id, 신규는 기록 날짜),
 * 한 경로에 둘을 섞으면 "id 가 없는 편집" 과 "생성" 을 구분할 수 없다.
 */
data class TimelineEventCreatePage(
    val recordDate: LocalDate,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            path = PATH,
            args = mapOf(RECORD_DATE_ARG to recordDate.toString()),
        )

    companion object {
        const val PATH = "/timeline/event/new"
        const val RECORD_DATE_ARG = "recordDate"

        fun recordDateFrom(args: Map<String, String>): LocalDate? =
            args[RECORD_DATE_ARG]?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
    }
}

data class TimelineEventEditorPage(
    val timelineEventId: Long,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            path = PATH,
            args = mapOf(TIMELINE_EVENT_ID_ARG to timelineEventId.toString()),
        )

    companion object {
        const val PATH = "/timeline/event/edit"
        const val TIMELINE_EVENT_ID_ARG = "timelineEventId"

        fun timelineEventIdFrom(args: Map<String, String>): Long? = args[TIMELINE_EVENT_ID_ARG]?.toLongOrNull()
    }
}

/**
 * 타임라인 초안 생성 전 데이터 전송 확인·동의 화면.
 *
 * 전송 스냅샷은 nav args 로 직렬화하지 않고 feature:home 의 인메모리 준비 상태로 전달한다.
 */
data object DraftConsentPage : Page {
    const val PATH = "/home/draft-consent"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 초안 생성 로딩. 표시 대상은 인자가 아니라 현재 활성 작업이 정본이라 인자를 받지 않는다. */
data object DraftLoadingPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = "/draft-loading"
}

/**
 * 동의 화면에서 데이터 유형 1개의 실제 전송 항목을 확인하는 상세 화면.
 *
 * 유형 이름만 인자로 나르고, 전송 스냅샷은 동의 화면과 같은 인메모리 준비 상태를 공유한다.
 */
data class DraftConsentDetailPage(
    val typeGroup: String,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            path = PATH,
            args = mapOf(TYPE_GROUP_ARG to typeGroup),
        )

    companion object {
        const val PATH = "/home/draft-consent/detail"
        const val TYPE_GROUP_ARG = "typeGroup"

        fun typeGroupFrom(args: Map<String, String>): String? = args[TYPE_GROUP_ARG]
    }
}

data object SettingsPage : Page {
    const val PATH = "/settings"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

data object CalendarPage : Page {
    const val PATH = "/calendar"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

data object ReflectionPage : Page {
    const val PATH = "/reflection"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

data object CollectionPage : Page {
    const val PATH = "/collection"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
