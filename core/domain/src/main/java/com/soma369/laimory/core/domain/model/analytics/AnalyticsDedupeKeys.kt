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

    /**
     * 완료 판정 키. 회원 구분은 **뒤에** 붙인다 — 지울 때는 회원을 모를 수 있어서, 날짜만으로 만든
     * 뿌리 키 하나로 그 날짜의 모든 회원 판정을 함께 지울 수 있어야 한다([timelineCompletedRoot]).
     *
     * 회원 식별자를 모르면(조회 실패·식별자를 안 주는 서버) 설치 단위인 뿌리 키를 그대로 쓴다.
     */
    fun timelineCompleted(
        recordDate: LocalDate,
        userId: Long?,
    ): AnalyticsDedupeKey {
        val root = timelineCompletedRoot(recordDate)
        return if (userId == null) root else AnalyticsDedupeKey("${root.value}:$userId")
    }

    /** 그 날짜 완료 판정의 뿌리. 지울 때 쓴다. */
    fun timelineCompletedRoot(recordDate: LocalDate): AnalyticsDedupeKey = AnalyticsDedupeKey("timeline_completed:$recordDate")

    /**
     * 설치 유입 확정 판정 키. 설치마다 새로 만드는 [installId] 를 붙인다 — 판정 기록은 백업에서 되살아날 수
     * 있는데, 고정 키면 재설치한 설치의 확정 이벤트가 옛 판정에 막힌다.
     */
    fun installAttributionResolved(installId: String): AnalyticsDedupeKey = AnalyticsDedupeKey("install_attribution_resolved:$installId")
}
