package com.soma369.laimory.reminder

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPushDelivery
import com.soma369.laimory.core.domain.model.analytics.AnalyticsPushType
import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        private val notifier: DailyReminderNotifications,
        private val analyticsHelper: AnalyticsHelper,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) {
        /**
         * 기록 읽기 → 중복 판정 → 표시 → 기록 저장을 한 번에 하나만 한다.
         *
         * 같은 시각에 건 알람 둘이 함께 오면 각자 코루틴으로 돈다. 나눠 돌면 둘 다 "아직 안 띄움"을 읽고 둘 다
         * 띄운다 — 같은 알림 자리를 덮어써도 소리·진동은 두 번 난다.
         */
        private val deliveryLock = Mutex()

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
                        deliveryLock.withLock { deliverIfDue(scheduledAt, now) }
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
            val recordDate = DailyReminderSchedule.recordDateAt(scheduledAt)
            // 같은 시각에 건 알람 둘 중 늦은 쪽이거나, 이미 받은 날 시각을 옮긴 경우다.
            if (memory.lastDeliveredRecordDate == recordDate) return
            if (DailyReminderSchedule.isTooLate(scheduledAt, now)) {
                Logger.i(LogDomain.PUSH, "일일 리마인더가 너무 늦게 와 건너뜀")
                return
            }
            val copy =
                if (memory.lastSavedRecordDate == recordDate) {
                    notifier.pickCompleted(memory.lastCompletedCopyIndex)
                } else {
                    notifier.pickGeneral(memory.lastGeneralCopyIndex)
                }
            if (!notifier.notify(copy, recordDate)) {
                Logger.i(LogDomain.PUSH, "기기 알림이 꺼져 있어 일일 리마인더를 띄우지 못함")
                return
            }
            // 띄운 직후에 센다. 아래 기록 저장이 실패해도 알림은 이미 떴다.
            analyticsHelper.log(
                AnalyticsEvent.PushShown(
                    AnalyticsPushType.DAILY_REMINDER,
                    AnalyticsPushDelivery.LOCAL,
                    copy.toAnalyticsCopy(notifier.titleOf(copy)),
                ),
            )
            repository.setLastDeliveredRecordDate(recordDate)
            when (copy) {
                is DailyReminderCopy.General -> repository.setLastGeneralCopyIndex(copy.index)
                is DailyReminderCopy.Completed -> repository.setLastCompletedCopyIndex(copy.index)
            }
            Logger.i(LogDomain.PUSH, "일일 리마인더 표시(${if (copy is DailyReminderCopy.Completed) "완료" else "일반"})")
        }
    }
