package com.soma369.laimory.feature.home.component

import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.DataSourceStatus
import com.soma369.laimory.feature.home.state.DraftConsentTypeGroup
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

/** 건수 단위. 확인 다이얼로그와 같은 표를 써서 카드와 다이얼로그가 같은 것을 다르게 세지 않는다. */
private val HomeSourceKind.countUnit: String
    get() =
        when (this) {
            HomeSourceKind.PHOTO -> DraftConsentTypeGroup.PHOTO
            HomeSourceKind.CALENDAR -> DraftConsentTypeGroup.CALENDAR
            HomeSourceKind.LOCATION -> DraftConsentTypeGroup.LOCATION
            HomeSourceKind.NOTIFICATION -> DraftConsentTypeGroup.NOTIFICATION
        }.countUnit

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
 * 규칙은 `N / M`(보낼 수 / 후보 수, Figma `Home / SourceCard`)이고, 낭독은 `후보 M장 중 N장` 처럼 단위를 붙인다
 * (단위는 확인 다이얼로그와 같다 — 사진 장·일정 개·위치 건·알림 건). 다만 **볼 것도 권한도 없을 때만** 문구를 바꾼다 — 도트가 꺼졌어도
 * 모인 것이 있으면 건수를 보여 주는 편이 사용자가 아는 것에 가깝다. 지원하지 않는 기기에는
 * 허용하라고 하지 않는다.
 *
 * 빈 문구는 유형을 말하지 않는다 — 내용 슬롯이 이미 `일정이 없어요` 라 적고 있어, 본문까지
 * 유형을 되풀이하면 카드에 같은 말이 두 줄로 선다.
 */
internal fun HomeUiState.cardBody(kind: HomeSourceKind): HomeCardBody {
    val count = countOf(kind)
    val status = statusOf(kind)
    return when {
        status == DataSourceStatus.UNSUPPORTED -> HomeCardBody.Message("이 기기에서는 지원하지 않아요")
        count.candidate > 0 ->
            HomeCardBody.Count(
                sending = count.sending,
                candidate = count.candidate,
                spoken = kind.countUnit.let { "${count.candidate}$it 중 ${count.sending}$it" },
            )
        status != DataSourceStatus.GRANTED -> HomeCardBody.Message("탭하여 허용")
        else -> HomeCardBody.Message("아직 모인 것이 없어요")
    }
}

/**
 * 사진 카드 격자 자리에 띄울 빈 문구.
 *
 * 전체 허용이고 지금 기록 창의 후보를 불러왔는데 0장일 때만이다. 권한이 없거나 일부만 허용이면 본문이
 * 이미 `탭하여 허용` 을 말하고, 그때 "갤러리에 없다" 고 하면 틀린 말이 된다.
 *
 * **다시 불러오는 중에는 알던 결과를 유지한다.** 카드를 눌러 시트를 열거나 화면에 돌아오면 같은 창을 다시
 * 불러오는데, 그동안 문구를 거두면 빈 회색 칸이 잠깐 드러난다. 창이 바뀌면 불러옴 표시가 내려가므로
 * 다른 기간의 결과가 남지는 않는다.
 */
internal fun HomeUiState.photoEmptyMessage(): String? =
    PHOTO_EMPTY_MESSAGE.takeIf {
        permissions.photo == DataSourceStatus.GRANTED &&
            hasLoadedPhotoCandidates &&
            availablePhotos.isEmpty()
    }

private const val PHOTO_EMPTY_MESSAGE = "갤러리에 이 기간 사진이 없어요"

/**
 * 분류 행 오른쪽, `>` 앞에 적을 지금 할 일. 사진 카드에서 고를 사진이 있는데 하나도 고르지 않았을 때 `사진 고르기`.
 *
 * 사진은 미리 골라 두지 않으면 빠진 채 만들어지는데, 카드만 보고는 고를 수 있다는 것을 놓친다. 바꿀 수 없는 날
 * (생성 중·완성된 날)에는 적지 않는다 — 눌러도 고를 수 없다.
 */
internal fun HomeUiState.cardHint(kind: HomeSourceKind): String? =
    PHOTO_PICK_HINT.takeIf {
        kind == HomeSourceKind.PHOTO && summary.photo.candidate > 0 && selectedPhotoIds.isEmpty() && !isInputLocked
    }

private const val PHOTO_PICK_HINT = "사진 고르기"

/** 카드 본체 탭. 갈 곳이 없으면 null 이라 눌리지 않는다. */
internal fun HomeUiState.cardClick(
    kind: HomeSourceKind,
    onIntent: (HomeUiIntent) -> Unit,
    onRequestPermission: (DataPermission) -> Unit,
): (() -> Unit)? =
    when (homeSourceTapTarget(countOf(kind).candidate, statusOf(kind))) {
        HomeSourceTapTarget.DETAIL -> ({ onIntent(HomeUiIntent.OpenSourceDetail(kind)) })
        HomeSourceTapTarget.PERMISSION -> ({ onRequestPermission(kind.permission()) })
        HomeSourceTapTarget.NONE -> null
    }

/** 카드는 상세로 가는데 권한은 덜 열린 경우의 보조 어포던스. */
internal fun HomeUiState.permissionAction(
    kind: HomeSourceKind,
    onRequestPermission: (DataPermission) -> Unit,
): (() -> Unit)? {
    if (!showsPermissionAction(countOf(kind).candidate, statusOf(kind))) return null
    return { onRequestPermission(kind.permission()) }
}
