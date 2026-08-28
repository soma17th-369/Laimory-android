package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import java.time.LocalDate

/**
 * 하루 기록을 서버에서 다시 읽어 세션을 통째로 갈아 끼운다.
 *
 * 이벤트를 만들거나 고친 뒤에 부른다. 응답 하나로 세션을 국소 수정하면 **서버가 계산하는 것들이
 * 조용히 어긋난다** — 시간을 고쳐도 목록 위치가 그대로인 것이 그 예다(제자리 치환이라 인덱스가
 * 안 바뀐다). 화면이 세션을 구독하고 편집기에서 돌아와도 재조회하지 않으므로, 어긋난 값이 그대로
 * 남는다.
 *
 * **실패는 삼킨다.** 서버 반영은 이미 끝났고 사용자가 한 일은 성공했다. 재조회가 안 됐다고
 * 편집을 실패로 되돌리면 같은 편집을 다시 하게 된다 — 화면은 다음 진입에서 맞춰진다.
 */
internal suspend fun refreshTimelineRecordSession(
    recordDate: LocalDate?,
    repository: TimelineRecordRepository,
    sessionRepository: TimelineRecordSessionRepository,
) {
    if (recordDate == null) return
    runCatching { repository.getDailyRecord(recordDate) }
        .onSuccess(sessionRepository::save)
}
