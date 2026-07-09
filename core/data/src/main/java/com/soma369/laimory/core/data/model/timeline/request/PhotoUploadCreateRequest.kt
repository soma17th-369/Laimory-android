package com.soma369.laimory.core.data.model.timeline.request

import kotlinx.serialization.Serializable

/** `POST /timeline/drafts/photo-uploads` 요청. presigned 업로드 URL 발급을 요청한다. */
@Serializable
data class PhotoUploadCreateRequest(
    val photos: List<PhotoUploadItem>,
)

/**
 * 업로드할 사진 1건의 발급 파라미터.
 *
 * [contentType]·[size] 는 presign 서명에 묶이므로 실제 S3 PUT 의 `Content-Type`/`Content-Length`
 * 와 정확히 같아야 한다(불일치 시 403).
 */
@Serializable
data class PhotoUploadItem(
    val contentType: String,
    val size: Long,
)
