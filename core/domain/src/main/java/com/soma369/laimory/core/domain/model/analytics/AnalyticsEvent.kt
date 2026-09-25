package com.soma369.laimory.core.domain.model.analytics

/**
 * 제품 분석으로 보낼 수 있는 이벤트.
 *
 * sealed 라 임의 이름을 만들 수 없고, 속성은 이벤트마다 타입으로 고정한다 — 자유 문자열을 허용하면
 * 오타와 원문 유출이 컴파일을 통과한다. 전송 이름·속성 이름은 도메인이 알지 않고
 * data 레이어의 매퍼가 정한다.
 *
 * **보내지 않는 것**: 기록·메모·질문·감정 원문, 사진·좌표·주소, 일정·알림·건강 원문, 실제 record date,
 * 사용자·record·Event·task 내부 ID, 예외 메시지와 스택트레이스. 숫자 속성도 이 목록을 따른다 —
 * 집계값만 싣는다. 날짜는 실제 값 대신 오늘과의 관계([AnalyticsRecordDayRelation])로만 보낸다.
 *
 * 기록 시점은 클릭이 아니라 **화면 표시 또는 서버 성공이 확정된 시점**이다.
 */
sealed interface AnalyticsEvent {
    /** 권한 요청 창을 열기 직전. */
    data class PermissionRequestStarted(
        val permission: AnalyticsPermissionType,
        val promptContext: AnalyticsPromptContext,
    ) : AnalyticsEvent

    /** 권한 요청 결과를 실제 권한 상태로 확인한 시점. */
    data class PermissionResult(
        val permission: AnalyticsPermissionType,
        val state: AnalyticsPermissionState,
        val promptContext: AnalyticsPromptContext,
    ) : AnalyticsEvent

    /** 설치 후 처음으로 생성할 재료가 생긴 시점. 설치당 1회. */
    data class DataCollectionReady(
        val trigger: AnalyticsReadyTrigger,
    ) : AnalyticsEvent

    /** 생성 버튼을 눌러 준비를 시작했다. 범위 오류 같은 입력 가드는 통과한 뒤다. */
    data class TimelineCreateStarted(
        val recordDayRelation: AnalyticsRecordDayRelation,
    ) : AnalyticsEvent

    /** 준비를 시작했지만 생성 요청까지 가지 않고 멈췄다. 이유마다 고칠 곳이 다르다. */
    data class TimelineCreateStopped(
        val reason: AnalyticsCreateStopReason,
        val recordDayRelation: AnalyticsRecordDayRelation,
    ) : AnalyticsEvent

    /** 보낼 데이터를 마지막으로 확인하는 창을 띄웠다. */
    data class TimelineEventReviewStarted(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val initialItemCount: Int,
    ) : AnalyticsEvent

    /**
     * 확인 창에서 만들기를 골랐다.
     *
     * [initialCounts] 는 앱이 보내려고 모은 후보, [finalCounts] 는 사용자가 미리 빼 둔 항목을 걸러 실제로 보낸
     * 목록이다. 사진도 센다 — 사진은 이 단계에서 뺄 수 없어 뺀 수에는 영향이 없고, 자동 수집만의 제외율은
     * 사진 묶음을 빼고 계산하면 된다.
     */
    data class TimelineEventReviewCompleted(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val initialCounts: AnalyticsItemCounts,
        val finalCounts: AnalyticsItemCounts,
    ) : AnalyticsEvent {
        /** 최종 상태로만 센다 — 뺐다가 다시 넣은 항목은 0 이다. */
        val netRemovedItemCount: Int get() = initialCounts.total - finalCounts.total
    }

    /** 서버가 생성 요청을 접수해 작업을 돌려줬다. */
    data class TimelineCreateRequested(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val itemCount: Int,
    ) : AnalyticsEvent

    /** 사진 업로드나 생성 요청 접수가 실패했다. */
    data class TimelineCreateRequestFailed(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val failureCode: AnalyticsFailureCode,
    ) : AnalyticsEvent

    /** 접수된 생성 작업이 끝났다. 작업당 1회. 실패일 때만 [failureCode] 가 있다. */
    data class TimelineCreateResult(
        val result: AnalyticsCreateResult,
        val failureCode: AnalyticsFailureCode?,
    ) : AnalyticsEvent

    /** 타임라인 조회가 성공해 화면에 보였다. */
    data class TimelineOpened(
        val timelineState: AnalyticsTimelineState,
        val recordDayRelation: AnalyticsRecordDayRelation,
    ) : AnalyticsEvent

    /** 기록 완료 절차(감정 선택)를 시작했다. */
    data class TimelineCompletionStarted(
        val recordDayRelation: AnalyticsRecordDayRelation,
    ) : AnalyticsEvent

    /**
     * 서버에서 `DRAFT → SAVED` 가 확정됐다. record day 당 1회 — 여기까지 오면 Activation 이다.
     *
     * 완료 순간의 이벤트 요약(메모·수정·삭제·직접 추가 건수)을 함께 싣는다.
     */
    data class TimelineCompleted(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val completionOutcome: AnalyticsCompletionOutcome,
        val eventSummary: AnalyticsTimelineEventSummary,
    ) : AnalyticsEvent

    /** 완료 저장이 실패해 사용자에게 알렸다. */
    data class TimelineCompletionFailed(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val failureCode: AnalyticsFailureCode,
    ) : AnalyticsEvent

    /**
     * 설치 유입 조회 결과가 확정됐다. 설치당 1회.
     *
     * 사용자 속성과 별개로 설치 시점의 원값·상태를 대조하는 기준이다. 캠페인 값은 결과가 캠페인을 실을 때만 있다.
     */
    data class InstallAttributionResolved(
        val attribution: InstallAttribution,
    ) : AnalyticsEvent
}
