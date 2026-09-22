package com.soma369.laimory.core.domain.model.analytics

import java.time.LocalDate

/**
 * 한 번만 보내는 이벤트의 판정 키. 키는 기기에만 남고 전송되지 않는다.
 *
 * 기기에서 일어나는 사건(수집 준비·생성 작업)은 설치 단위고, 회원의 기록에 관한 사건(완료)은 회원
 * 식별자를 알면 회원 단위로 가른다. 설치 단위로 두면 한 기기에서 계정을 바꿨을 때 두 번째 계정의 같은
 * 날짜 완료가 첫 계정 판정에 막혀 나가지 않는다.
 */
object AnalyticsDedupeKeys {
    /** 저장 항목과 사진 권한 두 경로가 같은 키를 써서, 먼저 온 쪽 하나만 나간다. */
    val DATA_COLLECTION_READY = AnalyticsDedupeKey("data_collection_ready")

    fun timelineCreateResult(taskId: String): AnalyticsDedupeKey = AnalyticsDedupeKey("timeline_create_result:$taskId")

    /** 회원 식별자를 모르면(조회 실패·식별자를 안 주는 서버) 설치 단위로 되돌아간다. */
    fun timelineCompleted(
        recordDate: LocalDate,
        userId: Long?,
    ): AnalyticsDedupeKey =
        if (userId == null) {
            AnalyticsDedupeKey("timeline_completed:$recordDate")
        } else {
            AnalyticsDedupeKey("timeline_completed:$userId:$recordDate")
        }
}
