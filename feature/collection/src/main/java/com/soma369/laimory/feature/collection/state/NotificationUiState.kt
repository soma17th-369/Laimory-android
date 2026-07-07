package com.soma369.laimory.feature.collection.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.NotificationCollectMode
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.source.InstalledApp
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class NotificationUiState(
    /** 저장소 첫 emission 전까지 로딩으로 둔다. */
    val isLoading: Boolean = true,
    /** 수집 모드(모든/선택). */
    val mode: NotificationCollectMode = NotificationCollectMode.SELECTIVE,
    /** 선택 수집 키워드 필터. */
    val keywords: Set<String> = emptySet(),
    /** 선택 수집 앱 allowlist(패키지명). */
    val allowedPackages: Set<String> = emptySet(),
    /** 스테이징된 알림(저장소 관찰 결과에서 NOTIFICATION 만 필터). */
    val stagedNotifications: List<SourceItem> = emptyList(),
    /** 앱 allowlist 선택용 설치앱 목록. */
    val installedApps: List<InstalledApp> = emptyList(),
) : UiState
