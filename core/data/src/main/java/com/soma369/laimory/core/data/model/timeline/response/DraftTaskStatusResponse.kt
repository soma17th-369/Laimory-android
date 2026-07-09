package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatus
import kotlinx.serialization.Serializable

/**
 * `GET /timeline/drafts/{taskId}` 응답.
 *
 * 생성 완료 여부 확인용이라 [status]·[error] 만 도메인으로 매핑한다. 성공 결과(`result`)는
 * 서버가 enrich 한 별도 스키마라 표시 기능에서 다룬다(여기선 무시).
 */
@Serializable
data class DraftTaskStatusResponse(
    val status: String,
    val error: String? = null,
)

internal fun DraftTaskStatusResponse.toDomain(): DraftTaskSnapshot =
    DraftTaskSnapshot(
        status =
            DraftTaskStatus.entries.firstOrNull { it.name == status }
                ?: throw ApiException.UnknownException("알 수 없는 초안 작업 상태입니다: $status"),
        error = error,
    )
