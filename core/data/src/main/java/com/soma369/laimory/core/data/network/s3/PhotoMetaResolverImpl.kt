package com.soma369.laimory.core.data.network.s3

import android.content.Context
import androidx.core.net.toUri
import com.soma369.laimory.core.domain.exception.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** [ContentResolver] 로 MIME/바이트 크기를 산출한다. size 는 스트리밍 PUT 의 `Content-Length` 와 같은 값이다. */
class PhotoMetaResolverImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PhotoMetaResolver {
        override fun resolve(clientPhotoUri: String): PhotoMeta {
            val uri = clientPhotoUri.toUri()
            val resolver = context.contentResolver
            val contentType =
                resolver.getType(uri)
                    ?: throw ApiException.UnknownException("사진 MIME 타입을 확인할 수 없습니다: $clientPhotoUri")
            val size =
                resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
                    ?.takeIf { it >= 0L }
                    ?: throw ApiException.UnknownException("사진 크기를 확인할 수 없습니다: $clientPhotoUri")
            return PhotoMeta(contentType = contentType, size = size)
        }
    }
