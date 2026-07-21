package com.soma369.laimory.core.domain.model.timeline

import com.soma369.laimory.core.domain.exception.ApiException

/** Android 타임라인 흐름에서 분기하는 서버 오류 코드. UI에서 raw 문자열을 직접 비교하지 않는다. */
enum class TimelineErrorCode(
    val serverValue: String?,
) {
    BAD_REQUEST("ERROR_0400"),
    RECORD_NOT_FOUND("ERROR_0404"),
    DRAFT_TASK_NOT_FOUND("ERROR_1001"),
    RECORD_ALREADY_SAVED("ERROR_1003"),
    PHOTO_COUNT_EXCEEDED("ERROR_1004"),
    PHOTO_SIZE_EXCEEDED("ERROR_1005"),
    UNSUPPORTED_PHOTO_FORMAT("ERROR_1007"),
    AI_REPORTED_FAILURE("ERROR_1008"),
    AI_DISPATCH_FAILURE("ERROR_1009"),
    STAGING_DATA_MISSING("ERROR_1010"),
    FINALIZE_FAILURE("ERROR_1011"),
    NO_NEW_TIMELINE_ITEM("ERROR_1013"),
    RETRYABLE_GEOCODING_FAILURE("ERROR_1014"),
    PERMANENT_GEOCODING_FAILURE("ERROR_1015"),
    DATE_OPERATION_IN_PROGRESS("ERROR_1016"),
    PHOTO_DELETE_FAILURE("ERROR_1017"),
    AUTHENTICATION_REQUIRED("ERROR_2001"),
    UNKNOWN(null),
    ;

    companion object {
        /** 폴링 FAILED body처럼 미지 코드도 상태로 보존해야 하는 곳에서 사용한다. */
        fun fromServerValue(value: String): TimelineErrorCode = recognizedOrNull(value) ?: UNKNOWN

        /** HTTP 오류처럼 미지 코드를 기존 공통 정책으로 fallback해야 하는 곳에서 사용한다. */
        fun recognizedOrNull(value: String?): TimelineErrorCode? = entries.firstOrNull { it.serverValue != null && it.serverValue == value }
    }
}

/** [ApiException.errorCode]를 타입 안전한 타임라인 오류로 해석한다. */
val ApiException.timelineErrorCode: TimelineErrorCode?
    get() = TimelineErrorCode.recognizedOrNull(errorCode)
