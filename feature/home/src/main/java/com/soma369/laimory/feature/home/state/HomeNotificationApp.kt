package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/**
 * 알림 카드가 보여 주는 앱 한 개와 그 건수.
 *
 * 아이콘은 화면이 [packageName] 으로 읽는다(`core:ui` 의 `rememberAppIcon`). 삭제된 앱은 읽히지
 * 않으므로 화면이 기본 아이콘으로 떨어뜨린다.
 *
 * 표시명은 **가장 최근 알림의 수집 당시 이름**이다 — 앱 이름이 바뀌었을 수 있어 저장된 값을 쓴다.
 * 순서는 건수 내림차순, 같으면 `packageName` 오름차순이다(회전이 흔들리지 않아야 한다).
 */
@Immutable
data class HomeNotificationApp(
    val packageName: String,
    val appName: String,
    val count: Int,
)
