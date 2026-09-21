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
 * 집계값만 싣는다.
 *
 * 화면에 심는 일(호출부)은 후속 이슈가 맡는다. 여기 정의는 통로를 검증할 첫 스키마다.
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
}
