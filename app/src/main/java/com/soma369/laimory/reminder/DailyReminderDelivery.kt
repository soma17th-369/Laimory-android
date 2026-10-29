package com.soma369.laimory.reminder

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알람을 받아 알림을 띄우고 다음 날로 다시 건다.
 *
 * 띄우기 직전에 조건을 다시 본다. 알람은 예약할 때의 조건으로 걸렸으므로, 그사이 로그아웃했거나 껐으면 띄우지
 * 않는다(예약도 이때 함께 내려간다).
 */
@Singleton
class DailyReminderDelivery
    @Inject
    constructor(
        private val repository: DailyReminderRepository,
        private val coordinator: DailyReminderCoordinator,
        private val notifier: DailyReminderNotifier,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) {
        /** 수신기가 부른다. 끝나면 [onFinished] 로 알린다 — 수신기는 그때까지 프로세스를 붙잡는다. */
        fun onAlarm(
            scheduledAtMillis: Long?,
            onFinished: () -> Unit,
        ) {
            applicationScope.launch {
                try {
                    if (coordinator.currentActiveTime() != null) {
                        val now = LocalDateTime.now()
                        val scheduledAt =
                            scheduledAtMillis
                                ?.let { millis -> LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()) }
                                ?: now
                        deliverIfDue(scheduledAt, now)
                    }
                    coordinator.resync()
                } catch (error: Exception) {
                    Logger.w(LogDomain.PUSH, "일일 리마인더 처리 실패: ${error::class.simpleName}")
                } finally {
                    onFinished()
                }
            }
        }

        private suspend fun deliverIfDue(
            scheduledAt: LocalDateTime,
            now: LocalDateTime,
        ) {
            val memory = repository.deliveryMemory()
            val scheduledDate = scheduledAt.toLocalDate()
            // 같은 시각에 건 알람 둘 중 늦은 쪽이다.
            if (memory.lastDeliveredDate == scheduledDate) return
            if (DailyReminderSchedule.isTooLate(scheduledAt, now)) {
                Logger.i(LogDomain.PUSH, "일일 리마인더가 너무 늦게 와 건너뜀")
                return
            }
            val recordDate = DailyReminderSchedule.recordDateAt(scheduledAt)
            val copy =
                if (memory.lastSavedRecordDate == recordDate) {
                    notifier.pickCompleted(memory.lastCompletedCopyIndex)
                } else {
                    notifier.pickGeneral(memory.lastGeneralTitleIndex)
                }
            if (!notifier.notify(copy, recordDate)) {
                Logger.i(LogDomain.PUSH, "기기 알림이 꺼져 있어 일일 리마인더를 띄우지 못함")
                return
            }
            repository.setLastDeliveredDate(scheduledDate)
            when (copy) {
                is DailyReminderCopy.General -> repository.setLastGeneralTitleIndex(copy.titleIndex)
                is DailyReminderCopy.Completed -> repository.setLastCompletedCopyIndex(copy.index)
            }
            Logger.i(LogDomain.PUSH, "일일 리마인더 표시(${if (copy is DailyReminderCopy.Completed) "완료" else "일반"})")
        }
    }
