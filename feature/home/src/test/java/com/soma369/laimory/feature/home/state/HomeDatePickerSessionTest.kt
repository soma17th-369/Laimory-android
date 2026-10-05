package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class HomeDatePickerSessionTest {
    private val saved = DefaultRecordRange.INITIAL
    private val session =
        HomeDatePickerSession(
            date = LocalDate.of(2026, 10, 5),
            startTime = LocalTime.of(5, 0),
            endDay = DraftEndDay.NEXT_DAY,
            endTime = LocalTime.of(6, 0),
        )

    @Test
    fun `체크하면 기본값 줄이 지금 고른 범위로 바뀐다`() {
        val checked = session.copy(saveAsDefault = true)

        assertEquals(checked.range, checked.defaultAfterConfirm(saved))
    }

    @Test
    fun `체크하지 않으면 기본값 줄은 저장된 기본값 그대로다`() {
        // 범위가 기본값과 달라 체크가 풀린 상태다.
        assertEquals(saved, session.defaultAfterConfirm(saved))
        assertEquals(saved, session.copy(saveAsDefault = false).defaultAfterConfirm(saved))
    }

    @Test
    fun `범위가 기본값과 같으면 둘이 같다`() {
        val same = session.copy(startTime = LocalTime.of(6, 0))

        assertEquals(saved, same.defaultAfterConfirm(saved))
    }
}
