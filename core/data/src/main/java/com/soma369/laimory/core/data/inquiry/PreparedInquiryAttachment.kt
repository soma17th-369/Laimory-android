package com.soma369.laimory.core.data.inquiry

import java.io.File

/**
 * 올릴 수 있게 바꾼 첨부 한 장. [contentType] · [size] 는 presign 발급과 S3 PUT 에 **같은 값**으로 쓴다.
 *
 * @param file 앱 캐시에 쓴 임시 파일. 보내기가 끝나면 성공·실패와 무관하게 지운다.
 */
data class PreparedInquiryAttachment(
    val file: File,
    val contentType: String,
    val size: Long,
) {
    /** `file:` 주소. S3 업로더는 ContentResolver 로 여는데, ContentResolver 는 file 스킴도 연다. */
    val uri: String get() = file.toURI().toString()
}
