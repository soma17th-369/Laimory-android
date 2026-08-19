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
