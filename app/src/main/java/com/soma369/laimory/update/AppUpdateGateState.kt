package com.soma369.laimory.update

/**
 * 앱을 열어도 되는지.
 *
 * 권장 업데이트는 여기 없다 — 앱을 **대신** 그리는 것이 아니라 위에 얹는 안내라서 별도 상태로
 * 둔다([AppUpdateGate.recommendation]).
 */
enum class AppUpdateGateState {
    /** 아직 확인 전. 판정이 끝날 때까지 앱도 강제 화면도 그리지 않는다. */
    CHECKING,

    /** 쓸 수 있다. */
    OPEN,

    /** 하한선 미만이라 업데이트 전에는 쓸 수 없다. */
    BLOCKED,
}
