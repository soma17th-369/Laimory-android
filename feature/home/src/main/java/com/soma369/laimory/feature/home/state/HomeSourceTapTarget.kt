package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.permission.DataSourceStatus

/** 원천 카드 본체를 눌렀을 때 가는 곳. */
enum class HomeSourceTapTarget {
    /** 유형 상세·사진 시트. */
    DETAIL,

    /** 권한 요청 또는 설정 이동. */
    PERMISSION,

    /** 아무 데도 가지 않는다. 이 기기에서는 열 방법이 없다. */
    NONE,
}

/**
 * 카드 진입은 **데이터 기준**이다. 도트와 묶지 않는다.
 *
 * 도트가 꺼지면 무조건 권한 흐름으로 보내면 부분 허용 사용자에게서 기능을 빼앗는다 — 사진을 일부만
 * 허용한 사용자는 읽을 수 있는 후보가 있고, 위치 전경 권한만 있는 사용자도 이미 수집된 체류·이동이
 * 있다. 시스템의 "허용 사진 재선택"과 "초안에 넣을 사진 고르기"는 다른 작업이다.
 *
 * 열 상세가 있으면 연다. 볼 것도 없고 권한도 없을 때만 권한 흐름이다.
 */
internal fun homeSourceTapTarget(
    candidateCount: Int,
    status: DataSourceStatus,
): HomeSourceTapTarget =
    when {
        // 허용할 방법이 없는 기기다. `탭하여 허용` 은 막다른 길이 된다.
        status == DataSourceStatus.UNSUPPORTED -> HomeSourceTapTarget.NONE
        candidateCount > 0 -> HomeSourceTapTarget.DETAIL
        status == DataSourceStatus.GRANTED -> HomeSourceTapTarget.DETAIL
        else -> HomeSourceTapTarget.PERMISSION
    }

/**
 * 카드 본체는 상세로 가는데 권한은 덜 열린 경우에만 보조 어포던스(`허용 →`)를 둔다.
 *
 * 카드 본체가 이미 권한 흐름이면 같은 일을 두 번 두는 셈이고, 다 허용됐거나 미지원이면 물을 것이 없다.
 */
internal fun showsPermissionAction(
    candidateCount: Int,
    status: DataSourceStatus,
): Boolean =
    status != DataSourceStatus.GRANTED &&
        status != DataSourceStatus.UNSUPPORTED &&
        homeSourceTapTarget(candidateCount, status) == HomeSourceTapTarget.DETAIL
