package com.soma369.laimory.core.collection.collector

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
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
        /**
         * 사진 EXIF 에서 GPS 좌표 `[lat, lng]` 를 읽는다. 좌표가 없으면 성공한 null, 읽지 못하면(권한 없음/IO)
         * 실패다.
         *
         * 여기서 로그를 남기지 않는다. 사진마다 도는 루프 안이라, 권한이 없으면 같은 줄이 사진 수만큼
         * 찍혀 브레드크럼 창을 밀어낸다. 호출부가 [PhotoExifFailureTally] 로 모아 한 줄로 남긴다.
         */
        fun read(baseUri: Uri): Result<DoubleArray?> {
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
            }
        }
    }
