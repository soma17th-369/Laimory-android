package com.soma369.laimory.core.data.network.s3

/**
 * 사진 1건의 업로드 메타. 같은 소스에서 한 번만 산출해 presign 발급값과 S3 PUT 값을 일치시킨다
 * (불일치 시 S3 403).
 */
data class PhotoMeta(
    val contentType: String,
    val size: Long,
)
