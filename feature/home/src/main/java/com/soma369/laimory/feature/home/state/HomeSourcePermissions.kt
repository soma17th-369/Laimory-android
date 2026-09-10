package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/**
 * 원천별 권한 도트가 보는 값 — "우리가 희망하는 옵션이 켜져 있는가".
 *
 * 권한은 사용자가 언제든 바꾸므로 **캐시하지 않고 화면 복귀마다 다시 본다**. 그래서 판정은
 * 화면이 하고 여기에는 결과만 담는다.
 *
 * **도트는 카드 진입 가능 여부와 묶이지 않는다.** 사진 일부 선택·위치 전경 권한만 있는 사용자도
 * 이미 읽을 수 있는 데이터가 있으므로 카드는 열려야 한다.
 */
@Immutable
data class HomeSourcePermissions(
    /** 사진 **전체** 허용. 일부 선택은 꺼짐이다. */
    val photo: Boolean = false,
    val calendar: Boolean = false,
    /**
     * 위치 **항상 허용**.
     *
     * 활동 인식은 보지 않는다 — 그것은 이동수단 추론에만 쓰이고 없으면 평균 속도로 보완하므로
     * 수집 자체는 멀쩡하다. 항상 허용을 다 해 준 사용자에게 회색 도트를 띄우면 무엇을 더 해야
     * 하는지 알 수 없다.
     */
    val location: Boolean = false,
    val notification: Boolean = false,
    /**
     * 알림 접근 설정 화면이 있는 기기인가.
     *
     * 없으면 허용할 방법이 아예 없다. `탭하여 허용` 을 띄우면 막다른 길이 되므로 화면이
     * 지원하지 않는다는 안내로 갈라야 한다.
     */
    val isNotificationSupported: Boolean = true,
)
