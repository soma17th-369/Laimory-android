package com.soma369.laimory.core.data.network.s3

import android.content.Context
import androidx.core.net.toUri
import com.soma369.laimory.core.data.di.S3Client
import com.soma369.laimory.core.domain.exception.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * S3 presigned PUT 업로더.
 *
 * 우리 서버 envelope/인터셉터/인증 헤더를 타지 않는 별도 [S3Client] OkHttpClient 를 쓴다
 * (특히 debug `MockInterceptor` 가 S3 요청을 가로채면 안 된다). `Authorization`/AWS key 는 넣지 않는다
 * — 서명 정보는 presigned URL 안에 있다.
 */
class S3PhotoUploaderImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @S3Client private val client: OkHttpClient,
    ) : S3PhotoUploader {
        override suspend fun upload(
            clientPhotoUri: String,
            uploadUrl: String,
            contentType: String,
            size: Long,
        ) = withContext(Dispatchers.IO) {
            val body =
                ContentUriRequestBody(
                    resolver = context.contentResolver,
                    uri = clientPhotoUri.toUri(),
                    contentType = contentType.toMediaType(),
                    size = size,
                )
            val request = Request.Builder().url(uploadUrl).put(body).build()

            val response =
                try {
                    client.newCall(request).execute()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    throw ApiException.NetworkException()
                }

            response.use { res ->
                // 이슈 #120: S3 presigned PUT 성공 기준은 정확히 200 (다른 2xx 는 실패로 본다).
                if (res.code != 200) {
                    throw ApiException.fromCode(res.code, "S3 사진 업로드 실패 (기대 200, 실제 ${res.code})")
                }
            }
        }
    }
