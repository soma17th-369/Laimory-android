package com.soma369.laimory.core.domain.exception

/**
 * Event 편집기가 직접 복구하거나 안내해야 하는 기능 단위 실패.
 *
 * 서버 오류 코드는 생성·수정 UseCase 경계에서 이 의미로 변환하며 Presentation 에는 노출하지 않는다.
 * 생성과 수정이 같은 화면을 쓰므로 사유도 함께 쓴다 — 나누면 같은 실패에 화면이 두 벌로 반응한다.
 */
class TimelineEventUpdateException(
    val reason: Reason,
    override val cause: ApiException,
) : Exception(cause.message, cause) {
    enum class Reason {
        INVALID_REQUEST,
        PHOTO_LIMIT_EXCEEDED,

        /** 수정은 대상 Event 가, 생성은 대상 하루 기록이 없다. 둘 다 편집기를 열어 둘 수 없다. */
        EVENT_UNAVAILABLE,
        DATE_OPERATION_IN_PROGRESS,
    }

    companion object {
        /**
         * 화면이 다룰 수 있는 실패면 그 사유로, 아니면 null 로 옮긴다.
         *
         * 생성과 수정이 서로 다른 코드를 쓴다 — 진행 중인 날짜 작업이 수정은 `-1016`, 생성은
         * `-1019` 다. 한 표에 모아 두어야 한쪽만 빠뜨리는 일이 없다.
         */
        fun reasonOf(errorCode: Int?): Reason? =
            when (errorCode) {
                INVALID_REQUEST -> Reason.INVALID_REQUEST
                PHOTO_LIMIT_EXCEEDED -> Reason.PHOTO_LIMIT_EXCEEDED
                RECORD_OR_EVENT_UNAVAILABLE -> Reason.EVENT_UNAVAILABLE
                UPDATE_DATE_OPERATION_IN_PROGRESS, CREATE_DATE_OPERATION_IN_PROGRESS -> Reason.DATE_OPERATION_IN_PROGRESS
                else -> null
            }

        private const val INVALID_REQUEST = -400
        private const val PHOTO_LIMIT_EXCEEDED = -1004
        private const val RECORD_OR_EVENT_UNAVAILABLE = -404
        private const val UPDATE_DATE_OPERATION_IN_PROGRESS = -1016
        private const val CREATE_DATE_OPERATION_IN_PROGRESS = -1019
    }
}
