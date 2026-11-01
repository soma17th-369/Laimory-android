package com.soma369.laimory.core.domain.usecase.reminder

import com.soma369.laimory.core.domain.usecase.FakeDailyReminderRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class IsDailyReminderDeliveredTodayUseCaseTest {
    private val reminder = FakeDailyReminderRepository()
    private val useCase = IsDailyReminderDeliveredTodayUseCase(reminder)

    @Test
    fun `오늘 기록 날짜에 띄웠으면 이미 받은 날이다`() =
        runBlocking {
            reminder.setLastDeliveredRecordDate(LocalDate.of(2026, 11, 2))

            assertTrue(useCase(LocalDateTime.of(2026, 11, 2, 22, 0)))
        }

    @Test
    fun `어제 띄운 것은 오늘 받은 것이 아니다`() =
        runBlocking {
            reminder.setLastDeliveredRecordDate(LocalDate.of(2026, 11, 1))

            assertFalse(useCase(LocalDateTime.of(2026, 11, 2, 22, 0)))
        }

    @Test
    fun `새벽은 아직 어제 기록이라 어제 받았으면 받은 날이다`() =
        runBlocking {
            // 06시 전은 어제 기록이다. 예약과 같은 규칙이어야 안내와 실제 예약이 어긋나지 않는다.
            reminder.setLastDeliveredRecordDate(LocalDate.of(2026, 11, 1))

            assertTrue(useCase(LocalDateTime.of(2026, 11, 2, 1, 0)))
        }

    @Test
    fun `한 번도 띄운 적 없으면 받은 날이 아니다`() =
        runBlocking {
            assertFalse(useCase(LocalDateTime.of(2026, 11, 2, 22, 0)))
        }
}
