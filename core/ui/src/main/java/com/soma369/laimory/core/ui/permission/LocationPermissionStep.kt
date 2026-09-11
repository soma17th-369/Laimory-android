package com.soma369.laimory.core.ui.permission

/**
 * 위치 권한이 완성되기까지 남은 단계.
 *
 * 위치만 한 번에 못 받는다. Android 가 전경 → 백그라운드 순서를 강제하고(둘을 함께 요청하면
 * 요청 전체를 무시한다), Android 11+ 는 백그라운드를 다이얼로그 대신 이 앱의 위치 권한 화면으로
 * 받는다. 이동수단 인식은 또 다른 런타임 권한이다.
 *
 * 그래서 화면은 "허용/미허용" 이분법이 아니라 **지금 어느 단계인지**를 보고 버튼을 바꾼다.
 */
enum class LocationPermissionStep {
    /** 전경 위치부터. 이동수단 인식·알림도 이 요청에 함께 실린다. */
    FOREGROUND,

    /** 전경은 됐고 `항상 허용` 이 남았다. Android 11+ 는 요청하면 이 앱의 위치 권한 화면이 뜬다. */
    BACKGROUND,

    /** 1단계에서 이동수단 인식만 거부한 경우. */
    ACTIVITY,

    /** 더 받을 것이 없다. */
    GRANTED,
    ;

    /**
     * 백그라운드 수집이 실제로 도는 단계인지.
     *
     * [ACTIVITY] 를 포함한다 — 이동수단 인식은 없으면 속도 추론으로 폴백할 뿐 수집을 막지 않는다.
     * 이 판정을 [GRANTED] 하나로 좁히면, 이동수단 인식만 거부한 사용자에게 **수집은 도는데 끄는
     * 자리는 보이지 않는** 상태가 된다.
     */
    val collectsInBackground: Boolean get() = this == ACTIVITY || this == GRANTED
}

/**
 * 허용 상태에서 남은 단계를 고른다.
 *
 * 순수 함수로 둬 조합을 테스트로 고정한다. 이 판정이 틀리면 예외가 아니라 **버튼이 영영 같은
 * 자리에 머물러** 눌러도 아무 일이 없는 화면이 된다.
 *
 * @param hasForeground 정밀 또는 대략 위치 중 하나라도 허용됐는지. 대략만 허용한 사용자도 위치를
 *   쓸 수 있으므로 둘 다 요구하지 않는다.
 */
fun locationPermissionStep(
    hasForeground: Boolean,
    hasBackground: Boolean,
    hasActivityRecognition: Boolean,
): LocationPermissionStep =
    when {
        !hasForeground -> LocationPermissionStep.FOREGROUND
        !hasBackground -> LocationPermissionStep.BACKGROUND
        !hasActivityRecognition -> LocationPermissionStep.ACTIVITY
        else -> LocationPermissionStep.GRANTED
    }
