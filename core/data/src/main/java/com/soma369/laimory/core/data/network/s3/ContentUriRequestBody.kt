package com.soma369.laimory.core.data.network.s3

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException

/**
 * `content://` URI 의 원본 바이트를 그대로 스트리밍하는 [RequestBody].
 *
 * [contentLength] 를 presign 발급에 쓴 [size] 로 고정 보고해 S3 서명 헤더(`content-length`)와
 * 정확히 일치시킨다. 바디는 멀티파트/base64 가 아닌 순수 바이너리다.
 */
internal class ContentUriRequestBody(
    private val resolver: ContentResolver,
    private val uri: Uri,
    private val contentType: MediaType,
    private val size: Long,
) : RequestBody() {
    override fun contentType(): MediaType = contentType

    override fun contentLength(): Long = size

    override fun writeTo(sink: BufferedSink) {
        val input = resolver.openInputStream(uri) ?: throw IOException("사진 스트림을 열 수 없습니다: $uri")
        input.source().use { source -> sink.writeAll(source) }
    }
}
