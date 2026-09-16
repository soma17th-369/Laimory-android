package com.soma369.laimory.core.ui.component.timepicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RollerDeltaTrackerTest {
    @Test
    fun `가운데 칸이 바뀔 때마다 그만큼의 이동량을 바로 낸다`() {
        val tracker = RollerDeltaTracker(initialIndex = 10)

        assertEquals(1, tracker.onCenterChanged(11))
        assertEquals(1, tracker.onCenterChanged(12))
        assertEquals(-2, tracker.onCenterChanged(10))
        assertEquals(0, tracker.onCenterChanged(10))
    }

    @Test
    fun `칸마다 낸 이동량의 합은 처음과 마지막 칸의 차이와 같다`() {
        // 스크롤이 멈춘 뒤 한 번에 알리던 때와 값이 같아야 한다.
        val tracker = RollerDeltaTracker(initialIndex = 100)
        val path = listOf(101, 102, 103, 102, 104, 107, 106)

        val total = path.sumOf(tracker::onCenterChanged)

        assertEquals(106 - 100, total)
    }

    @Test
    fun `값을 따라 롤러를 옮기는 동안의 가운데 변화는 세지 않는다`() {
        val tracker = RollerDeltaTracker(initialIndex = 5)

        tracker.beginAlign(target = 9)
        assertEquals(0, tracker.onCenterChanged(6))
        assertEquals(0, tracker.onCenterChanged(8))
        assertEquals(0, tracker.onCenterChanged(9))
        tracker.endAlign()

        assertEquals(9, tracker.lastIndex)
        assertEquals(1, tracker.onCenterChanged(10))
    }

    @Test
    fun `자리 맞춤 도중 사용자가 잡아 굴리면 보이는 칸까지 한 번에 따라잡는다`() {
        val tracker = RollerDeltaTracker(initialIndex = 5)

        tracker.beginAlign(target = 9)
        tracker.onCenterChanged(7)
        // 사용자가 잡아 애니메이션이 취소됐다.
        tracker.endAlign()

        // 값은 9 를 가리키는데 롤러는 7 근처다. 한 칸 굴려 8 이 되면 값도 8 로 온다.
        assertEquals(-1, tracker.onCenterChanged(8))
    }
}
