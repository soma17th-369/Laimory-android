package com.soma369.laimory.feature.timeline.testexport

import android.content.Context
import android.net.Uri
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 임시 테스트 전용(삭제 예정) — 선택 날짜 하루의 수집 데이터를 JSON + 사진으로 Drive 에 내보낸다.
 *
 * `testexport` 패키지의 **유일한 진입점**. 나중에 이 패키지를 통째로 지우고
 * TimelineViewModel 의 호출 1곳만 placeholder 로 되돌리면 기능이 깔끔히 제거된다.
 */
class DriveTestExporter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val observeSourceItemsUseCase: ObserveSourceItemsUseCase,
    ) {
        private val zone: ZoneId = ZoneId.systemDefault()

        /** 제출 폴더명에 쓸 업로드 시각 포맷: `2026-07-09_14-30-15`. */
        private val uploadStampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

        /** 선택 [date] 하루를 Drive 로 내보내고, 결과 메시지(성공/실패 사유)를 반환한다. */
        suspend fun export(date: LocalDate): String {
            val configJson =
                DriveExportConfig.loadBundledOAuthJson(context)
                    ?: return "Drive 자격증명(drive_oauth.json)이 없어 내보낼 수 없어요. (debug 전용)"

            val token =
                DriveOAuth.getAccessTokenFromJson(configJson).getOrElse {
                    return "토큰 발급 실패: ${it.message}"
                }

            val window = RecordDateWindow.ofDate(date, zone)
            // 서버 초안과 동일하게 시간순(오래된→최신)으로 내보낸다 — 표시용 observeAll 은 최신순(DESC).
            val items =
                observeSourceItemsUseCase()
                    .first()
                    .filter { window.contains(it) }
                    .sortedBy { it.startAt }
            if (items.isEmpty()) return "$date 창에 내보낼 수집 데이터가 없어요."

            // 사진 바이트를 먼저 확정한다. JSON 은 여기서 성공한 사진만 링크하므로
            // JSON ↔ photos/ 정합성이 깨지지 않는다(읽기 실패분은 photoFile=null 로 남고 메시지로 보고).
            val resolved = resolvePhotos(items)
            val json = DayBundleJson.build(date, zone, items) { resolved.fileNameByRawId[it.rawId] }

            return DriveUploader
                .uploadDayBundle(
                    token = token,
                    rootFolderId = DriveExportConfig.ROOT_FOLDER_ID,
                    parentFolderName = DriveExportConfig.PARENT_FOLDER_NAME,
                    dateFolderName = date.toString(),
                    submissionFolderName = "export-${LocalDateTime.now(zone).format(uploadStampFormatter)}",
                    jsonFileName = "$date.json",
                    json = json,
                    photos = resolved.uploads,
                ).fold(
                    onSuccess = { summary ->
                        if (resolved.readFailures.isEmpty()) {
                            "$date 내보내기 완료 — $summary"
                        } else {
                            "$date 내보내기 완료 — $summary " +
                                "(사진 ${resolved.readFailures.size}장 읽기 실패로 JSON·업로드에서 제외: " +
                                "${resolved.readFailures.joinToString()})"
                        }
                    },
                    onFailure = { "내보내기 실패: ${it.message}" },
                )
        }

        /** 확정된 사진 업로드 목록 + rawId→파일명 링크 + 읽기 실패 파일명. */
        private class ResolvedPhotos(
            val uploads: List<DriveUploader.PhotoUpload>,
            val fileNameByRawId: Map<String, String>,
            val readFailures: List<String>,
        )

        /**
         * PHOTO 아이템의 바이트를 읽어 업로드 목록을 확정한다(순번 접두로 파일명 중복 방지).
         * 읽기/다운스케일 실패분은 [ResolvedPhotos.readFailures] 로 분리해 조용히 누락되지 않게 한다.
         */
        private fun resolvePhotos(items: List<SourceItem>): ResolvedPhotos {
            val uploads = mutableListOf<DriveUploader.PhotoUpload>()
            val fileNameByRawId = mutableMapOf<String, String>()
            val readFailures = mutableListOf<String>()

            items
                .filter { it.itemType == ItemType.PHOTO }
                .forEachIndexed { index, item ->
                    val payload = item.payload as? PhotoPayload ?: return@forEachIndexed
                    val base = payload.fileName.ifBlank { "${item.rawId}.jpg" }
                    val name = "${index.toString().padStart(3, '0')}_$base"
                    val bytes = readPhotoBytes(Uri.parse(payload.clientPhotoUri))
                    if (bytes == null) {
                        readFailures += name
                    } else {
                        fileNameByRawId[item.rawId] = name
                        uploads += DriveUploader.PhotoUpload(fileName = name, bytes = bytes)
                    }
                }
            return ResolvedPhotos(uploads, fileNameByRawId, readFailures)
        }

        /** 다운스케일 토글에 따라 사진 바이트를 읽는다. 실패 시 null. */
        private fun readPhotoBytes(uri: Uri): ByteArray? =
            if (DriveExportConfig.DOWNSCALE_PHOTOS) {
                PhotoDownscaler.downscaleJpeg(
                    context,
                    uri,
                    DriveExportConfig.DOWNSCALE_MAX_DIMENSION,
                    DriveExportConfig.DOWNSCALE_JPEG_QUALITY,
                )
            } else {
                PhotoDownscaler.readOriginal(context, uri)
            }
    }
