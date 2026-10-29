package com.soma369.laimory.core.domain.model.reminder

import java.time.LocalDate

/**
 * 알림을 띄울 때 고를 문구에 필요한 기억.
 *
 * @param lastSavedRecordDate 이 기기에서 마지막으로 작성 완료한 기록 날짜. 같은 날이면 완료 문구를 쓴다.
 *   다른 기기에서 저장한 날은 알 수 없다.
 * @param lastGeneralCopyIndex 일반 문구에서 마지막으로 쓴 짝. 다음 날 같은 짝을 피한다.
 * @param lastCompletedCopyIndex 완료 문구에서 마지막으로 쓴 짝.
 * @param lastDeliveredRecordDate 마지막으로 알림을 띄운 **기록 날짜**. 하루치 기록에 알림은 한 번이다 — 띄운 뒤 시각을
 *   바꿔도 같은 기록 날짜면 다시 띄우지 않는다. 달력 날짜로 세면 21시에 받고 새벽 1시로 옮겼을 때 같은 하루를
 *   두 번 권한다.
 */
data class DailyReminderDeliveryMemory(
    val lastSavedRecordDate: LocalDate?,
    val lastGeneralCopyIndex: Int?,
    val lastCompletedCopyIndex: Int?,
    val lastDeliveredRecordDate: LocalDate?,
)
