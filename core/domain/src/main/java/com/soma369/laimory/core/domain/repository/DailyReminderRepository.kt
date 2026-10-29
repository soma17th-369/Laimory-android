package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.reminder.DailyReminderDeliveryMemory
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * 기기에서 예약하는 일일 리마인더의 로컬 상태. 서버와 동기화하지 않는다.
 *
 * 값은 두 갈래다. 켜짐·시각·문구 기억은 **기기** 값이라 계정이 바뀌어도 남는다. 서버 넘겨받기 표시·전체 알림
 * 거울값·작성 완료 날짜는 **계정** 값이라 새 세션이 시작되면 비운다([NewSessionPreparer]).
 *
 * 계정 값은 네트워크를 기다린 뒤에 쓰는 경우가 많다. 그사이 로그아웃·재로그인이 지나가면 앞 세션의 늦은 응답이 새
 * 세션 값으로 적힌다. 그래서 계정 값을 쓰는 쪽은 시작할 때 [accountGeneration] 을 읽어 두고, 쓸 때 그 세대를 함께
 * 넘긴다. 세대가 바뀌었으면 쓰지 않는다.
 *
 * 기록 보조값(완료 날짜·문구 기억·전체 알림 거울값)은 쓰다 실패해도 호출부를 실패시키지 않는다 — 알림
 * 문구나 표시 판정이 한 번 어긋나는 것보다, 저장·설정 변경 같은 본래 동작이 막히는 쪽이 크다.
 */
interface DailyReminderRepository : NewSessionPreparer {
    /** 저장한 값. 저장한 적이 없거나 읽지 못하면 [DailyReminderSettings.DEFAULT]. */
    val settings: Flow<DailyReminderSettings>

    /**
     * 지금 세션 계정의 서버 리마인더를 끄는 일이 끝났는지.
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

    /** 지금 세션의 세대. 새 세션이 시작될 때마다 바뀐다. 계정 값을 쓸 때 넘긴다. */
    suspend fun accountGeneration(): Long

    /** 켜짐을 저장한 적이 있는지. 없을 때만 서버의 예전 선택을 이어받는다. */
    suspend fun hasStoredEnabled(): Boolean

    suspend fun setEnabled(isEnabled: Boolean)

    suspend fun setTime(time: LocalTime)

    /** [generation] 이 지금 세대일 때만 적는다. 적었으면 `true`. */
    suspend fun markServerReminderHandedOff(generation: Long): Boolean

    /** [generation] 이 지금 세대일 때만 적는다. */
    suspend fun setPushEnabledMirror(
        isEnabled: Boolean,
        generation: Long,
    )

    suspend fun deliveryMemory(): DailyReminderDeliveryMemory

    /** [generation] 이 지금 세대일 때만 적는다. */
    suspend fun markRecordSaved(
        recordDate: LocalDate,
        generation: Long,
    )

    suspend fun setLastGeneralCopyIndex(index: Int)

    suspend fun setLastCompletedCopyIndex(index: Int)

    suspend fun setLastDeliveredRecordDate(date: LocalDate)
}
