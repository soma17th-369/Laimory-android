package com.soma369.laimory.core.data.network.s3

/** presigned URL 로 사진 원본 바이트를 S3 에 순수 바이너리 PUT 한다. */
interface S3PhotoUploader {
    /**
     * [clientPhotoUri] 의 원본 바이트를 [uploadUrl] 로 PUT 한다.
     *
     * [contentType]·[size] 는 presign 발급에 쓴 값과 반드시 같아야 한다(S3 서명 일치). 200 이 아니면
     * `ApiException` 으로 던진다.
     */
    suspend fun upload(
        clientPhotoUri: String,
        uploadUrl: String,
        contentType: String,
        size: Long,
    )
}
