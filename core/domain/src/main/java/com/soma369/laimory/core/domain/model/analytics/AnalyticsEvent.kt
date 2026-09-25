package com.soma369.laimory.core.domain.model.analytics

import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 제품 분석으로 보낼 수 있는 이벤트.
 *
 * sealed 라 임의 이름을 만들 수 없고, 속성은 이벤트마다 타입으로 고정한다 — 자유 문자열을 허용하면
 * 오타와 원문 유출이 컴파일을 통과한다. 전송 이름·속성 이름은 도메인이 알지 않고
 * data 레이어의 매퍼가 정한다.
 *
 * **보내지 않는 것**: 기록·메모·질문·감정 원문, 사진·좌표·주소, 일정·알림·건강 원문,
 * 사용자·record·task 내부 ID, 예외 메시지와 스택트레이스. 숫자 속성도 이 목록을 따른다 — 집계값만 싣는다.
 *
 * GA4 스펙이 싣기로 한 값은 보낸다: 기록 날짜(`record date`), 편집 이벤트의 사건 ID 와 사건 시작 시각.
 * 오늘과의 관계([AnalyticsRecordDayRelation])도 그대로 함께 싣는다 — 날짜·시각의 경계는 모두 서울 기준이다.
 * 메모는 원문 대신 글자 수만 싣는다.
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
        val recordDate: LocalDate,
        val entryPoint: AnalyticsEntryPoint,
    ) : AnalyticsEvent

    /** 준비를 시작했지만 생성 요청까지 가지 않고 멈췄다. 이유마다 고칠 곳이 다르다. */
    data class TimelineCreateStopped(
        val reason: AnalyticsCreateStopReason,
        val recordDayRelation: AnalyticsRecordDayRelation,
        val recordDate: LocalDate,
    ) : AnalyticsEvent

    /** 보낼 데이터를 마지막으로 확인하는 창을 띄웠다. */
    data class TimelineEventReviewStarted(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val recordDate: LocalDate,
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
        val recordDate: LocalDate,
        val initialCounts: AnalyticsItemCounts,
        val finalCounts: AnalyticsItemCounts,
    ) : AnalyticsEvent {
        /** 최종 상태로만 센다 — 뺐다가 다시 넣은 항목은 0 이다. */
        val netRemovedItemCount: Int get() = initialCounts.total - finalCounts.total
    }

    /** 서버가 생성 요청을 접수해 작업을 돌려줬다. */
    data class TimelineCreateRequested(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val recordDate: LocalDate,
        val itemCount: Int,
    ) : AnalyticsEvent

    /** 사진 업로드나 생성 요청 접수가 실패했다. */
    data class TimelineCreateRequestFailed(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val recordDate: LocalDate,
        val failureCode: AnalyticsFailureCode,
    ) : AnalyticsEvent

    /**
     * 접수된 생성 작업이 끝났다. 작업당 1회.
     *
     * 성공일 때만 [recordDate] 와 [eventCount](만들어진 타임라인의 사건 수)가, 실패일 때만 [failureCode] 가 있다.
     */
    data class TimelineCreateResult(
        val result: AnalyticsCreateResult,
        val failureCode: AnalyticsFailureCode? = null,
        val recordDate: LocalDate? = null,
        val eventCount: Int? = null,
    ) : AnalyticsEvent

    /** 타임라인 조회가 성공해 화면에 보였다. */
    data class TimelineOpened(
        val timelineState: AnalyticsTimelineState,
        val recordDayRelation: AnalyticsRecordDayRelation,
        val recordDate: LocalDate,
        val entryPoint: AnalyticsEntryPoint,
    ) : AnalyticsEvent

    /** 완료한 지난 기록(오늘 이전 날짜)을 열었다. [TimelineOpened] 와 함께 나간다. */
    data class TimelinePastRecordOpened(
        val recordAgeBucket: AnalyticsRecordAgeBucket,
        val entryPoint: AnalyticsEntryPoint,
        val recordDate: LocalDate,
    ) : AnalyticsEvent

    /** 기록 완료 절차(감정 선택)를 시작했다. */
    data class TimelineCompletionStarted(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val recordDate: LocalDate,
    ) : AnalyticsEvent

    /**
     * 서버에서 `DRAFT → SAVED` 가 확정됐다. record day 당 1회 — 여기까지 오면 Activation 이다.
     *
     * 완료 순간의 이벤트 요약(메모·수정·삭제·직접 추가 건수)을 함께 싣는다.
     */
    data class TimelineCompleted(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val recordDate: LocalDate,
        val completionOutcome: AnalyticsCompletionOutcome,
        val eventSummary: AnalyticsTimelineEventSummary,
    ) : AnalyticsEvent

    /** 완료 저장이 실패해 사용자에게 알렸다. */
    data class TimelineCompletionFailed(
        val recordDayRelation: AnalyticsRecordDayRelation,
        val recordDate: LocalDate,
        val failureCode: AnalyticsFailureCode,
    ) : AnalyticsEvent

    /**
     * 사건 메모 저장이 서버에서 성공했다. 사건마다 모아 두었다가 기록을 완료하거나 화면을 나갈 때 한 번 보낸다.
     *
     * [memoLength] 는 마지막으로 저장한 메모의 글자 수다. 0 이면 메모를 지웠다.
     */
    data class TimelineMemoSaved(
        val target: AnalyticsTimelineEventTarget,
        val memoLength: Int,
    ) : AnalyticsEvent {
        companion object {
            /** 원문 대신 싣는 글자 수. 이모지처럼 두 칸을 쓰는 글자도 한 글자로 센다. */
            fun lengthOf(memo: String?): Int = memo?.let { it.codePointCount(0, it.length) } ?: 0
        }
    }

    /**
     * 사건 수정이 서버에서 성공했다. 바뀐 칸이 없으면 보내지 않는다.
     *
     * [eventStartAt] 은 수정한 뒤 사건의 시작 시각이다(서버와 같이 시간대 없는 벽시계 시각).
     */
    data class TimelineEventUpdated(
        val target: AnalyticsTimelineEventTarget,
        val eventStartAt: LocalDateTime,
        val changedFields: Set<AnalyticsEventField>,
    ) : AnalyticsEvent {
        init {
            require(changedFields.isNotEmpty()) { "바뀐 칸이 없는 수정은 보내지 않는다." }
        }

        val updateScope: AnalyticsUpdateScope get() = requireNotNull(AnalyticsUpdateScope.of(changedFields))

        val changedFieldCount: Int get() = changedFields.size
    }

    /** 사건 하나를 지웠다. 하루 기록 전체를 지운 것은 아니다. */
    data class TimelineEventDeleted(
        val target: AnalyticsTimelineEventTarget,
    ) : AnalyticsEvent

    /** 편집 화면에서 새 사건을 직접 추가했다. */
    data class TimelineEventCreated(
        val eventType: TimelineEventType,
        val photoCount: Int,
        val recordState: AnalyticsTimelineState,
        val recordDate: LocalDate,
    ) : AnalyticsEvent

    /**
     * 가입했다. 로그인 직후 서버가 온보딩을 마치지 않은 계정이라고 답했으면 새 계정으로 **추정**한다 —
     * 서버가 신규 여부를 따로 주지 않는다. 온보딩을 끝내지 않은 채 재설치한 기존 계정도 여기 섞인다.
     */
    data class SignUp(
        val method: SocialLoginProvider,
    ) : AnalyticsEvent

    /**
     * 온보딩의 장이 보였다. 회차([flowId])마다 장별로 한 번.
     *
     * [flowId] 는 온보딩 회차를 잇는 무작위 토큰이다 — 사람도 기기도 가리키지 않는다.
     */
    data class OnboardingStepViewed(
        val flowId: String,
        val version: AnalyticsOnboardingVersion,
        val step: AnalyticsOnboardingStep,
        val stepIndex: Int,
        val entryMode: AnalyticsOnboardingEntryMode,
        val eligibility: AnalyticsOnboardingEligibility,
    ) : AnalyticsEvent

    /** 온보딩의 장에서 행동을 골랐다. 회차마다 장별로 처음 고른 것 하나만. */
    data class OnboardingStepAction(
        val flowId: String,
        val version: AnalyticsOnboardingVersion,
        val step: AnalyticsOnboardingStep,
        val action: AnalyticsOnboardingAction,
    ) : AnalyticsEvent
}
