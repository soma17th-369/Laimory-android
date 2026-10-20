package com.soma369.laimory.feature.timeline.viewmodel

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.TimelineRecordDeleteException
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineDeleteFailureActionTest {
    @Test
    fun `재시도 안내는 서버 문구를 먼저 쓴다`() {
        val cause = ApiException.ConflictException(errorCode = -1016, rawCode = 409, serverMessage = "서버가 정한 안내")
        val error = TimelineRecordDeleteException(TimelineRecordDeleteException.Reason.DATE_OPERATION_IN_PROGRESS, cause)

        assertEquals(TimelineDeleteFailureAction.Retryable("서버가 정한 안내"), error.toTimelineDeleteFailureAction())
    }

    @Test
    fun `서버 문구가 없으면 앱 문구로 대신한다`() {
        val cause = ApiException.ConflictException(errorCode = -1017, rawCode = 409)
        val error = TimelineRecordDeleteException(TimelineRecordDeleteException.Reason.PHOTO_DELETE_FAILED, cause)

        assertEquals(
            TimelineDeleteFailureAction.Retryable("서버 사진을 삭제하지 못했어요. 잠시 후 다시 시도해주세요."),
            error.toTimelineDeleteFailureAction(),
        )
    }

    @Test
    fun `코드로 갈리지 않는 실패도 서버 문구가 있으면 쓴다`() {
        val error = ApiException.fromCode(400, errorCode = -400, serverMessage = "잘못된 요청입니다.")

        assertEquals(TimelineDeleteFailureAction.Retryable("잘못된 요청입니다."), error.toTimelineDeleteFailureAction())
    }

    @Test
    fun `연결 실패는 서버 응답이 없으므로 앱 문구다`() {
        assertEquals(
            TimelineDeleteFailureAction.Retryable("네트워크 상태를 확인한 뒤 다시 시도해주세요."),
            ApiException.NetworkException().toTimelineDeleteFailureAction(),
        )
    }
}
