package com.soma369.laimory.feature.collection.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.source.InstalledApp
import com.soma369.laimory.core.ui.base.UiState

@Immutable
data class NotificationUiState(
    /** 저장소 첫 emission 전까지 로딩으로 둔다. */
    val isLoading: Boolean = true,
    /** 클릭 수집 활성화 여부. 현재 화면에서는 상태 안내에만 사용하고 설정 UI는 후속으로 분리한다. */
    val collectOnClick: Boolean = true,
    /** 앱이 내장한 기본 키워드를 함께 쓸지 여부. */
    val useDefaultKeywords: Boolean = true,
    /** 사용자가 직접 등록한 키워드 필터. 기본 키워드는 포함하지 않는다. */
    val keywords: Set<String> = emptySet(),
    /** 게시 알림을 수집할 앱 allowlist(패키지명). */
    val allowedPackages: Set<String> = emptySet(),
    /** 스테이징된 알림(저장소 관찰 결과에서 NOTIFICATION 만 필터). */
    val stagedNotifications: List<SourceItem> = emptyList(),
    /** 앱 allowlist 선택용 설치앱 목록. */
    val installedApps: List<InstalledApp> = emptyList(),
) : UiState
