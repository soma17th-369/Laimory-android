package com.soma369.laimory.feature.home.component

import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.feature.home.state.HomeSourceCount
import com.soma369.laimory.feature.home.state.HomeSourceKind
import com.soma369.laimory.feature.home.state.HomeSourceTapTarget
import com.soma369.laimory.feature.home.state.HomeUiIntent
import com.soma369.laimory.feature.home.state.HomeUiState
import com.soma369.laimory.feature.home.state.homeSourceTapTarget
import com.soma369.laimory.feature.home.state.isInputLocked
import com.soma369.laimory.feature.home.state.showsPermissionAction

/** 카드가 그 원천의 상태를 어디서 읽는지. */
internal fun HomeUiState.countOf(kind: HomeSourceKind): HomeSourceCount =
    when (kind) {
        HomeSourceKind.PHOTO -> summary.photo
        HomeSourceKind.CALENDAR -> summary.calendar
        HomeSourceKind.LOCATION -> summary.location
        HomeSourceKind.NOTIFICATION -> summary.notification
    }

internal fun HomeUiState.statusOf(kind: HomeSourceKind): DataSourceStatus =
    when (kind) {
        HomeSourceKind.PHOTO -> permissions.photo
        HomeSourceKind.CALENDAR -> permissions.calendar
        HomeSourceKind.LOCATION -> permissions.location
        HomeSourceKind.NOTIFICATION -> permissions.notification
    }

private fun HomeSourceKind.permission(): DataPermission =
    when (this) {
        HomeSourceKind.PHOTO -> DataPermission.PHOTO
        HomeSourceKind.CALENDAR -> DataPermission.CALENDAR
        HomeSourceKind.LOCATION -> DataPermission.LOCATION
        HomeSourceKind.NOTIFICATION -> DataPermission.NOTIFICATION_LISTENER
    }

/**
 * 카드 본문 한 줄.
 *
 * 규칙은 `후보 M개 중 N개` 다. 다만 **볼 것도 권한도 없을 때만** 문구를 바꾼다 — 도트가 꺼졌어도
 * 모인 것이 있으면 건수를 보여 주는 편이 사용자가 아는 것에 가깝다. 지원하지 않는 기기에는
 * 허용하라고 하지 않는다.
 *
 * 빈 문구는 유형을 말하지 않는다 — 내용 슬롯이 이미 `일정이 없어요` 라 적고 있어, 본문까지
 * 유형을 되풀이하면 카드에 같은 말이 두 줄로 선다.
 */
internal fun HomeUiState.cardBody(kind: HomeSourceKind): String {
    val count = countOf(kind)
    val status = statusOf(kind)
    return when {
        status == DataSourceStatus.UNSUPPORTED -> "이 기기에서는 지원하지 않아요"
        count.candidate > 0 -> "${count.candidate}개 중 ${count.sending}개"
        status != DataSourceStatus.GRANTED -> "탭하여 허용"
        else -> "아직 모인 것이 없어요"
    }
}

/** 카드 본체 탭. 갈 곳이 없으면 null 이라 눌리지 않는다. */
internal fun HomeUiState.cardClick(
    kind: HomeSourceKind,
    onIntent: (HomeUiIntent) -> Unit,
    onRequestPermission: (DataPermission) -> Unit,
): (() -> Unit)? {
    if (draftStatus.isInputLocked) return null
    return when (homeSourceTapTarget(countOf(kind).candidate, statusOf(kind))) {
        HomeSourceTapTarget.DETAIL -> ({ onIntent(HomeUiIntent.OpenSourceDetail(kind)) })
        HomeSourceTapTarget.PERMISSION -> ({ onRequestPermission(kind.permission()) })
        HomeSourceTapTarget.NONE -> null
    }
}

/** 카드는 상세로 가는데 권한은 덜 열린 경우의 보조 어포던스. */
internal fun HomeUiState.permissionAction(
    kind: HomeSourceKind,
    onRequestPermission: (DataPermission) -> Unit,
): (() -> Unit)? {
    if (draftStatus.isInputLocked) return null
    if (!showsPermissionAction(countOf(kind).candidate, statusOf(kind))) return null
    return { onRequestPermission(kind.permission()) }
}
