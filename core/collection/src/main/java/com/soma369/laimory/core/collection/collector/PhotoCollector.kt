package com.soma369.laimory.core.collection.collector

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.soma369.laimory.core.domain.collector.Collector
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * MediaStore 기반 사진 메타데이터 수집기.
 *
 * 권한 확인/요청은 하지 않는다([Collector] 계약) — 권한이 없어 `ContentResolver` query 가 실패(null 커서/
 * `SecurityException`)하면 빈 목록을 반환한다. 실제 권한 요청은 UI 계층(`:feature:collection`)이 담당한다.
 *
 * 원본 파일은 복사하지 않고 메타데이터와 `clientPhotoUri` 만 담는다. 위치는 EXIF 에서 읽는다
 * (Android 10+ 는 MediaStore 위치가 redact 되므로 `setRequireOriginal` + `ACCESS_MEDIA_LOCATION` 필요).
 * 재스캔 중복 저장은 저장 계층의 `(itemType, sourceName, sourceKey)` 멱등이 막으므로 dedup 은 신경 쓰지 않는다.
 *
 * 조회·EXIF 파싱은 blocking I/O 이므로 [Dispatchers.IO] 에서 수행한다.
 */
internal class PhotoCollector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Collector {
        override val itemType: ItemType = ItemType.PHOTO

        override suspend fun collect(): List<SourceItem> =
            withContext(Dispatchers.IO) {
                val collectedAt = Instant.now()
                val zoneId = ZoneId.systemDefault()
                queryRows().mapNotNull { row ->
                    row.toSourceItem(
                        rawId = UUID.randomUUID().toString(),
                        collectedAt = collectedAt,
                        zoneId = zoneId,
                    )
                }
            }

        /**
         * MediaStore 를 최신순으로 조회해 최대 [BATCH_SIZE] 건을 [PhotoMediaRow] 로 읽는다.
         * 권한 미허용/조회 실패 시 빈 목록.
         */
        private fun queryRows(): List<PhotoMediaRow> {
            val cursor =
                runCatching {
                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        PROJECTION,
                        null,
                        null,
                        "${MediaStore.Images.Media.DATE_ADDED} DESC",
                    )
                }.getOrElse { e ->
                    // 권한 미허용은 SecurityException 으로 온다 — 계약대로 빈 목록.
                    Logger.w(LogDomain.COLLECTION, "MediaStore 사진 조회 실패: ${e.message}")
                    null
                } ?: return emptyList()

            return cursor.use { readRows(it) }
        }

        private fun readRows(cursor: Cursor): List<PhotoMediaRow> {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            val rows = ArrayList<PhotoMediaRow>(minOf(cursor.count, BATCH_SIZE))
            while (cursor.moveToNext() && rows.size < BATCH_SIZE) {
                val id = cursor.getLong(idColumn)
                val baseUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val location = readExifLocation(baseUri)
                rows.add(
                    PhotoMediaRow(
                        id = id,
                        displayName = cursor.getString(nameColumn).orEmpty(),
                        clientPhotoUri = baseUri.toString(),
                        dateTakenMillis = cursor.getLong(dateTakenColumn).takeIf { !cursor.isNull(dateTakenColumn) },
                        dateAddedSeconds = cursor.getLong(dateAddedColumn).takeIf { !cursor.isNull(dateAddedColumn) },
                        latitude = location?.get(0),
                        longitude = location?.get(1),
                    ),
                )
            }
            return rows
        }

        /**
         * 사진 EXIF 에서 GPS 좌표 `[lat, lng]` 를 읽는다. 없거나 실패(권한 없음/EXIF 없음/IO)하면 null.
         *
         * Android 10(Q)+ 는 MediaStore 가 위치를 redact 하므로 [MediaStore.setRequireOriginal] 로 원본 URI 를
         * 얻어야 하고 `ACCESS_MEDIA_LOCATION` 권한이 필요하다. 그 이전 버전은 파일 EXIF 를 바로 읽는다.
         */
        private fun readExifLocation(baseUri: Uri): DoubleArray? {
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

        private companion object {
            /** MVP 배치 상한. 최신순 조회에서 앞 N 건만 읽어 시작 성능/메모리 부담을 제한한다. */
            const val BATCH_SIZE = 500

            val PROJECTION =
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_ADDED,
                )
        }
    }
