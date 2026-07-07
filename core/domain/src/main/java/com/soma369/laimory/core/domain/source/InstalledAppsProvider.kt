package com.soma369.laimory.core.domain.source

/** 알림 필터 앱 선택(allowlist) UI 를 위한 설치앱 목록 제공 포트. */
interface InstalledAppsProvider {
    /** 런처에 뜨는(실행 가능한) 앱 목록을 표시명 오름차순으로 반환한다. */
    suspend fun launchableApps(): List<InstalledApp>
}

/** 설치앱 한 건. 알림 allowlist 선택 표시용. */
data class InstalledApp(
    val packageName: String,
    val label: String,
)
