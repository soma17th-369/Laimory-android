package com.soma369.laimory.core.ui.component.timepicker

/**
 * 롤러 한 열이 값에 알릴 이동량을 센다.
 *
 * 가운데 칸이 바뀌는 **즉시** 이동량을 낸다. 스크롤이 멈출 때까지 기다리면 fling·snap 애니메이션이
 * 끝나는 수백 ms 동안 요약 줄은 이전 값에 머물고, 그 사이 확인을 누르면 가운데 보이던 값이 아니라
 * 이전 값으로 확정된다.
 *
 * 값을 따라 롤러를 옮기는 스크롤(자리 맞춤)은 사용자가 굴린 것이 아니므로 세지 않는다 — 세면 그 이동이
 * 다시 값으로 되먹임된다.
 */
internal class RollerDeltaTracker(
    initialIndex: Int,
) {
    /** 마지막으로 값에 반영한 가운데 칸. */
    var lastIndex: Int = initialIndex
        private set

    private var isAligning = false

    /** 가운데 칸이 [index] 가 됐다. 값에 알릴 이동량을 돌려주고, 알릴 것이 없으면 0 이다. */
    fun onCenterChanged(index: Int): Int {
        if (isAligning) return 0
        val delta = index - lastIndex
        lastIndex = index
        return delta
    }

    /**
     * 값이 가리키는 [target] 으로 롤러를 옮기기 시작한다. [endAlign] 까지의 가운데 변화는 세지 않는다.
     *
     * 도중에 사용자가 잡아 멈추면 기준점은 [target] 에 남는다. 그다음 굴린 자리까지의 이동량이 한 번에
     * 나가 값이 보이는 칸을 따라잡는다.
     */
    fun beginAlign(target: Int) {
        lastIndex = target
        isAligning = true
    }

    fun endAlign() {
        isAligning = false
    }
}
