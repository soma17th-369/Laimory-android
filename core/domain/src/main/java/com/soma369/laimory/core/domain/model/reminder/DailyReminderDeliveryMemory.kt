package com.soma369.laimory.core.domain.model.reminder

import java.time.LocalDate

/**
 * 알림을 띄울 때 고를 문구에 필요한 기억.
 *
 * @param lastSavedRecordDate 이 기기에서 마지막으로 작성 완료한 기록 날짜. 같은 날이면 완료 문구를 쓴다.
 *   다른 기기에서 저장한 날은 알 수 없다.
 * @param lastGeneralTitleIndex 일반 문구에서 마지막으로 쓴 제목. 다음 날 같은 제목을 피한다.
 * @param lastCompletedCopyIndex 완료 문구에서 마지막으로 쓴 짝.
 * @param lastDeliveredDate 마지막으로 알림을 띄운 예정 날짜. 같은 날 두 번 띄우지 않는다.
 */
data class DailyReminderDeliveryMemory(
    val lastSavedRecordDate: LocalDate?,
    val lastGeneralTitleIndex: Int?,
    val lastCompletedCopyIndex: Int?,
    val lastDeliveredDate: LocalDate?,
)
