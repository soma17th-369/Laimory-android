package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.reminder.DailyReminderDeliveryMemory
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * 기기에서 예약하는 일일 리마인더의 로컬 상태. 서버와 동기화하지 않는다.
 *
 * 기록 보조값(완료 날짜·문구 기억·전체 알림 거울값)은 쓰다 실패해도 호출부를 실패시키지 않는다 — 알림
 * 문구나 표시 판정이 한 번 어긋나는 것보다, 저장·설정 변경 같은 본래 동작이 막히는 쪽이 크다.
 */
interface DailyReminderRepository {
    /** 저장한 값. 저장한 적이 없거나 읽지 못하면 [DailyReminderSettings.DEFAULT]. */
    val settings: Flow<DailyReminderSettings>

    /**
     * 서버 리마인더를 끄는 일이 이 기기에서 한 번이라도 끝났는지.
     *
     * 끝나기 전에는 예약하지 않는다. 서버가 아직 보내는 동안 기기도 띄우면 같은 시각에 두 번 온다.
     */
    val isServerReminderHandedOff: Flow<Boolean>

    /**
     * 서버가 마지막으로 확인해 준 전체 알림 값. 확인한 적이 없으면 `null`.
     *
     * 주인은 서버지만 알림을 띄우는 순간에는 물어볼 수 없어 거울로 둔다. 판정에만 쓴다.
     */
    val pushEnabledMirror: Flow<Boolean?>

    /** 켜짐을 저장한 적이 있는지. 없을 때만 서버의 예전 선택을 이어받는다. */
    suspend fun hasStoredEnabled(): Boolean

    suspend fun setEnabled(isEnabled: Boolean)

    suspend fun setTime(time: LocalTime)

    suspend fun markServerReminderHandedOff()

    suspend fun setPushEnabledMirror(isEnabled: Boolean)

    suspend fun deliveryMemory(): DailyReminderDeliveryMemory

    suspend fun markRecordSaved(recordDate: LocalDate)

    suspend fun setLastGeneralTitleIndex(index: Int)

    suspend fun setLastCompletedCopyIndex(index: Int)

    suspend fun setLastDeliveredRecordDate(date: LocalDate)
}
