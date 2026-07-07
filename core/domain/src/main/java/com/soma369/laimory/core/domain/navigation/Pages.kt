package com.soma369.laimory.core.domain.navigation

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

data object Feature1Page : Page {
    const val PATH = "/feature1"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

data object TimelinePage : Page {
    const val PATH = "/timeline"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

data object SettingsPage : Page {
    const val PATH = "/settings"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

data object CollectionPage : Page {
    const val PATH = "/collection"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
