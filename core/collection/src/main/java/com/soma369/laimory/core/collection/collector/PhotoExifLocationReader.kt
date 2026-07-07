package com.soma369.laimory.core.collection.collector

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 사진 원본 EXIF 에서 GPS 좌표를 읽는 공용 리더.
 *
 * 전량 배치 수집(`PhotoCollector`)과 선택 수집(`PhotoMediaSource`) 이 동일한 EXIF 읽기 규칙을 공유하도록
 * 별도 컴포넌트로 분리했다.
 *
 * Android 10(Q)+ 는 MediaStore 가 위치를 redact 하므로 [MediaStore.setRequireOriginal] 로 원본 URI 를
 * 얻어야 하고 `ACCESS_MEDIA_LOCATION` 권한이 필요하다. 그 이전 버전은 파일 EXIF 를 바로 읽는다.
 */
internal class PhotoExifLocationReader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        /** 사진 EXIF 에서 GPS 좌표 `[lat, lng]` 를 읽는다. 없거나 실패(권한 없음/EXIF 없음/IO)하면 null. */
        fun read(baseUri: Uri): DoubleArray? {
            val uri =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.setRequireOriginal(baseUri)
                } else {
                    baseUri
                }
            return runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream).latLong
                }
            }.getOrElse { e ->
                Logger.w(LogDomain.COLLECTION, "사진 EXIF 위치 읽기 실패(uri=$baseUri): ${e.message}")
                null
            }
        }
    }
