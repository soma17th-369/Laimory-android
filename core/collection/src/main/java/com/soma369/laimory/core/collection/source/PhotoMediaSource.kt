package com.soma369.laimory.core.collection.source

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.soma369.laimory.core.collection.collector.PhotoExifLocationReader
import com.soma369.laimory.core.collection.collector.PhotoMediaRow
import com.soma369.laimory.core.collection.collector.toSourceItem
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.source.PhotoSource
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * MediaStore 기반 날짜 선택 사진 수집기([PhotoSource] 구현).
 *
 * 전량 배치 수집(`PhotoCollector`)과 달리, 날짜([photosOn])로 후보를 조회하고 사용자가 고른 id([collect])만
 * [SourceItem] 으로 변환한다. 권한 확인/요청은 하지 않으며([PhotoSource] 계약), 실패 시 빈 목록을 반환한다.
 *
 * 조회·EXIF 파싱은 blocking I/O 이므로 [Dispatchers.IO] 에서 수행하고, EXIF 위치는 [PhotoExifLocationReader]
 * 를 `PhotoCollector` 와 공유한다.
 */
internal class PhotoMediaSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val exifLocationReader: PhotoExifLocationReader,
    ) : PhotoSource {
        /**
         * [date](기기 시간대) 의 `DATE_TAKEN` 범위 `[그날 00:00, 다음날 00:00)` 에 촬영된 사진 후보를 최신순 반환.
         *
         * 날짜 기준은 `DATE_TAKEN` 이므로(설계) 촬영 시각이 없는 사진(스크린샷·다운로드 등)은 어떤 날짜에도 잡히지 않는다.
         */
        override suspend fun photosOn(date: LocalDate): List<PhotoCandidate> =
            withContext(Dispatchers.IO) {
                val zone = ZoneId.systemDefault()
                val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                queryCandidates(startMillis, endMillis)
            }

        override suspend fun collect(ids: List<Long>): List<SourceItem> =
            withContext(Dispatchers.IO) {
                if (ids.isEmpty()) return@withContext emptyList()
                val collectedAt = Instant.now()
                val zoneId = ZoneId.systemDefault()
                queryRowsByIds(ids).mapNotNull { row ->
                    row.toSourceItem(
                        rawId = UUID.randomUUID().toString(),
                        collectedAt = collectedAt,
                        zoneId = zoneId,
                    )
                }
            }

        private fun queryCandidates(
            startMillis: Long,
            endMillis: Long,
        ): List<PhotoCandidate> {
            val cursor =
                runCatching {
                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        CANDIDATE_PROJECTION,
                        "${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ?",
                        arrayOf(startMillis.toString(), endMillis.toString()),
                        "${MediaStore.Images.Media.DATE_TAKEN} DESC",
                    )
                }.getOrElse { e ->
                    // 권한 미허용은 SecurityException 으로 온다 — 계약대로 빈 목록.
                    Logger.w(LogDomain.COLLECTION, "MediaStore 사진 후보 조회 실패: ${e.message}")
                    null
                } ?: return emptyList()

            return cursor.use { readCandidates(it) }
        }

        private fun readCandidates(cursor: Cursor): List<PhotoCandidate> {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            val candidates = ArrayList<PhotoCandidate>(cursor.count)
            while (cursor.moveToNext()) {
                if (cursor.isNull(dateTakenColumn)) continue
                val id = cursor.getLong(idColumn)
                candidates.add(
                    PhotoCandidate(
                        id = id,
                        contentUri =
                            ContentUris
                                .withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                                .toString(),
                        takenAt = Instant.ofEpochMilli(cursor.getLong(dateTakenColumn)),
                    ),
                )
            }
            return candidates
        }

        /**
         * [ids] 를 [PhotoMediaRow] 로 읽는다. SQLite `IN (...)` 변수 상한을 넘지 않도록 [SQL_IN_LIMIT] 씩 쪼갠다.
         */
        private fun queryRowsByIds(ids: List<Long>): List<PhotoMediaRow> = ids.chunked(SQL_IN_LIMIT).flatMap(::queryRowsChunk)

        private fun queryRowsChunk(ids: List<Long>): List<PhotoMediaRow> {
            val placeholders = ids.joinToString(",") { "?" }
            val cursor =
                runCatching {
                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        ROW_PROJECTION,
                        "${MediaStore.Images.Media._ID} IN ($placeholders)",
                        ids.map(Long::toString).toTypedArray(),
                        null,
                    )
                }.getOrElse { e ->
                    Logger.w(LogDomain.COLLECTION, "MediaStore 사진 조회 실패(ids=${ids.size}): ${e.message}")
                    null
                } ?: return emptyList()

            return cursor.use { readRows(it) }
        }

        private fun readRows(cursor: Cursor): List<PhotoMediaRow> {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            val rows = ArrayList<PhotoMediaRow>(cursor.count)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val baseUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val location = exifLocationReader.read(baseUri)
                rows.add(
                    PhotoMediaRow(
                        id = id,
                        displayName = cursor.getString(nameColumn).orEmpty(),
                        clientPhotoUri = baseUri.toString(),
                        dateTakenMillis = if (cursor.isNull(dateTakenColumn)) null else cursor.getLong(dateTakenColumn),
                        dateAddedSeconds = if (cursor.isNull(dateAddedColumn)) null else cursor.getLong(dateAddedColumn),
                        latitude = location?.get(0),
                        longitude = location?.get(1),
                    ),
                )
            }
            return rows
        }

        private companion object {
            /** SQLite `IN (?, ...)` 의 변수 개수 상한(구버전 999) 아래로 잡은 청크 크기. */
            const val SQL_IN_LIMIT = 900

            val CANDIDATE_PROJECTION =
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_TAKEN,
                )

            val ROW_PROJECTION =
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_ADDED,
                )
        }
    }
