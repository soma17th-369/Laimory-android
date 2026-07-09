package com.soma369.laimory.core.data.model.timeline.response

import kotlinx.serialization.Serializable

/** `POST /timeline/drafts/photo-uploads` 응답. */
@Serializable
data class PhotoUploadCreateResponse(
    val uploads: List<PhotoUploadEntry>,
)

/**
 * 발급된 업로드 1건. 요청 `photos[]` 와 **같은 순서**로 내려온다(식별자가 없어 인덱스로 매칭).
 *
 * - [uploadUrl]: S3 presigned PUT 대상. 쿼리스트링을 재조립하지 않고 그대로 사용한다.
 * - [filename]: 서버가 부여한 파일명. 초안 생성 시 PHOTO payload 의 `filename` 에 넣는다.
 */
@Serializable
data class PhotoUploadEntry(
    val filename: String,
    val uploadUrl: String,
)
