package com.soma369.laimory.core.collection.source

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.soma369.laimory.core.collection.collector.CapturedPhotoPolicy
import com.soma369.laimory.core.collection.collector.PhotoExifFailureTally
import com.soma369.laimory.core.collection.collector.PhotoExifLocationReader
import com.soma369.laimory.core.collection.collector.PhotoMediaRow
import com.soma369.laimory.core.collection.collector.effectiveStartMillis
import com.soma369.laimory.core.collection.collector.toSourceItem
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.source.PhotoSource
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
 * MediaStore 기반 범위 선택 사진 수집기([PhotoSource] 구현).
 *
 * 전량 배치 수집(`PhotoCollector`)과 달리, 범위([photosIn])로 후보를 조회하고 사용자가 고른 id([collect])만
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
         * [window]의 반열린 구간에 포함되는 사진 후보를 최신순(유효 시각 기준)으로 반환한다.
         *
         * 날짜 기준은 `DATE_TAKEN`(촬영 시각) 우선, 없거나 0 이면 `DATE_ADDED`(추가 시각) fallback 이다.
         * 저장(toSourceItem)의 유효 시각 규칙과 같아 촬영 시각이 없는 스크린샷·다운로드도 추가 시각으로 조회된다.
         */
        override suspend fun photosIn(window: RecordDateWindow): List<PhotoCandidate> =
            withContext(Dispatchers.IO) {
                queryCandidates(
                    startMillis = window.start.toEpochMilli(),
                    endMillis = window.end.toEpochMilli(),
                )
            }

        /** [photosIn] 과 같은 구간·시각 규칙으로 읽되, [CapturedPhotoPolicy] 가 직접 찍은 사진으로 보는 것만 남긴다. */
        override suspend fun capturedPhotosIn(window: RecordDateWindow): List<PhotoCandidate> =
            withContext(Dispatchers.IO) {
                queryCandidates(
                    startMillis = window.start.toEpochMilli(),
                    endMillis = window.end.toEpochMilli(),
                    projection = CAPTURED_PROJECTION,
                    rowFilterOf = ::capturedRowFilter,
                )
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

        /** @param rowFilterOf 커서의 컬럼 위치를 한 번 잡아 행마다 남길지 정하는 판정을 만든다. 없으면 모두 남긴다. */
        private fun queryCandidates(
            startMillis: Long,
            endMillis: Long,
            projection: Array<String> = CANDIDATE_PROJECTION,
            rowFilterOf: ((Cursor) -> (Cursor) -> Boolean)? = null,
        ): List<PhotoCandidate> {
            // DATE_ADDED 는 seconds 단위이므로 범위 경계의 epoch millis 를 seconds 로 환산한다.
            val startSeconds = startMillis / MILLIS_PER_SECOND
            val endSeconds = endMillis / MILLIS_PER_SECOND
            val cursor =
                runCatching {
                    context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        CANDIDATE_SELECTION,
                        arrayOf(
                            startMillis.toString(),
                            endMillis.toString(),
                            startSeconds.toString(),
                            endSeconds.toString(),
                        ),
                        null,
                    )
                }.getOrElse { e ->
                    // 권한 미허용은 SecurityException 으로 온다 — 계약대로 빈 목록.
                    Logger.w(LogDomain.COLLECTION, "MediaStore 사진 후보 조회 실패: ${e::class.simpleName}")
                    null
                } ?: return emptyList()

            // 유효 시각(DATE_TAKEN·DATE_ADDED fallback) 기준 최신순 정렬은 커서 SQL 대신 메모리에서 한다
            // (초안 범위로 제한되어 작고, MediaStore 가 정렬 표현식을 제한하는 경우를 피한다).
            return cursor.use { readCandidates(it, rowFilterOf?.invoke(it)) }.sortedByDescending { it.takenAt }
        }

        private fun readCandidates(
            cursor: Cursor,
            rowFilter: ((Cursor) -> Boolean)?,
        ): List<PhotoCandidate> {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            val candidates = ArrayList<PhotoCandidate>(cursor.count)
            while (cursor.moveToNext()) {
                if (rowFilter != null && !rowFilter(cursor)) continue
                val dateTakenMillis = if (cursor.isNull(dateTakenColumn)) null else cursor.getLong(dateTakenColumn)
                val dateAddedSeconds = if (cursor.isNull(dateAddedColumn)) null else cursor.getLong(dateAddedColumn)
                // 저장(toSourceItem)과 같은 규칙으로 유효 시각을 정한다. 쿼리에서 걸러지지만 방어적으로 무효면 건너뛴다.
                val takenAtMillis = effectiveStartMillis(dateTakenMillis, dateAddedSeconds) ?: continue
                val id = cursor.getLong(idColumn)
                candidates.add(
                    PhotoCandidate(
                        id = id,
                        contentUri =
                            ContentUris
                                .withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                                .toString(),
                        takenAt = Instant.ofEpochMilli(takenAtMillis),
                    ),
                )
            }
            return candidates
        }

        /** [CAPTURED_PROJECTION] 으로 읽은 커서의 행이 직접 찍은 사진인지 판정한다. */
        private fun capturedRowFilter(cursor: Cursor): (Cursor) -> Boolean {
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(PATH_COLUMN)
            val captureInfoColumns = CAPTURE_INFO_COLUMNS.map(cursor::getColumnIndexOrThrow)
            return { row ->
                CapturedPhotoPolicy.isCaptured(
                    displayName = row.getString(nameColumn),
                    path = row.getString(pathColumn),
                    hasCaptureInfo = captureInfoColumns.takeIf { it.isNotEmpty() }?.any { !row.isNull(it) },
                )
            }
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
                    Logger.w(LogDomain.COLLECTION, "MediaStore 사진 조회 실패(ids=${ids.size}): ${e::class.simpleName}")
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
            val exifFailures = PhotoExifFailureTally()
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val baseUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val location =
                    exifLocationReader.read(baseUri).getOrElse { e ->
                        exifFailures.record(e)
                        null
                    }
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
            exifFailures.report(rows.size)
            return rows
        }

        private companion object {
            /** SQLite `IN (?, ...)` 의 변수 개수 상한(구버전 999) 아래로 잡은 청크 크기. */
            const val SQL_IN_LIMIT = 900

            /** `DATE_ADDED`(seconds) → millis 환산 계수. */
            const val MILLIS_PER_SECOND = 1_000L

            /**
             * DATE_TAKEN(우선)·DATE_ADDED(fallback) 중 유효 시각이 범위 `[start, end)` 에 드는 후보 선택.
             * 저장(toSourceItem)의 유효 시각 규칙과 일치시켜 촬영 시각이 없는 사진도 추가일로 잡는다.
             * API 30+ 가 정렬/선택 표현식을 제한하므로 산술·CASE 없이 컬럼 비교만 쓴다(DATE_ADDED 는 seconds 로 비교).
             * args: [startMillis, endMillis, startSeconds, endSeconds].
             */
            val CANDIDATE_SELECTION =
                "(${MediaStore.Images.Media.DATE_TAKEN} > 0 " +
                    "AND ${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ?) " +
                    "OR ((${MediaStore.Images.Media.DATE_TAKEN} IS NULL OR ${MediaStore.Images.Media.DATE_TAKEN} <= 0) " +
                    "AND ${MediaStore.Images.Media.DATE_ADDED} >= ? AND ${MediaStore.Images.Media.DATE_ADDED} < ?)"

            val CANDIDATE_PROJECTION =
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_ADDED,
                )

            /**
             * 사진이 놓인 경로. API 29 부터 `RELATIVE_PATH`(`DCIM/Camera/`)이고, 그 전에는 파일 전체 경로(`DATA`)뿐이다.
             * API 28 의 `DATA` 는 읽기만 하므로 저장소 정책과 무관하다.
             */
            @Suppress("DEPRECATION")
            val PATH_COLUMN: String =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA

            /** 촬영 정보 컬럼(API 30+). 하나라도 값이 있으면 카메라가 찍은 사진으로 본다. 그 전에는 없다. */
            val CAPTURE_INFO_COLUMNS: List<String> =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    listOf(MediaStore.Images.Media.ISO, MediaStore.Images.Media.EXPOSURE_TIME)
                } else {
                    emptyList()
                }

            val CAPTURED_PROJECTION: Array<String> =
                CANDIDATE_PROJECTION + MediaStore.Images.Media.DISPLAY_NAME + PATH_COLUMN + CAPTURE_INFO_COLUMNS

            val ROW_PROJECTION =
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_ADDED,
                )
        }
    }
