package com.soma369.laimory.core.domain.model.analytics

import java.time.LocalDate

/**
 * 한 번만 보내는 이벤트의 판정 키.
 *
 * 설치 단위다 — 분석용 사용자 ID 를 서버가 아직 주지 않아 사용자 단위로 가를 수 없다. 재설치하면 다시
 * 한 번 나갈 수 있다. ID 가 생기면 여기서 사용자 구분을 더한다. 키는 기기에만 남고 전송되지 않는다.
 */
object AnalyticsDedupeKeys {
    /** 저장 항목과 사진 권한 두 경로가 같은 키를 써서, 먼저 온 쪽 하나만 나간다. */
    val DATA_COLLECTION_READY = AnalyticsDedupeKey("data_collection_ready")

    fun timelineCreateResult(taskId: String): AnalyticsDedupeKey = AnalyticsDedupeKey("timeline_create_result:$taskId")

    fun timelineCompleted(recordDate: LocalDate): AnalyticsDedupeKey = AnalyticsDedupeKey("timeline_completed:$recordDate")
}
