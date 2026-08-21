package com.soma369.laimory.core.domain.exception

/** Event에 연결된 PHOTO Item 삭제 화면이 직접 안내해야 하는 기능 오류. */
class TimelineEventPhotoDeleteException(
    val reason: Reason,
    override val cause: ApiException,
) : Exception(cause.message, cause) {
    enum class Reason {
        ITEM_NOT_PHOTO,
    }
}
