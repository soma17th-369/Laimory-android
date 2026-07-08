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
            val items = observeSourceItemsUseCase().first().filter { window.contains(it) }
            if (items.isEmpty()) return "$date 창에 내보낼 수집 데이터가 없어요."

            val photoNames = photoFileNames(items)
            val json = DayBundleJson.build(date, zone, items) { photoNames[it.rawId] }
            val photos = buildPhotoUploads(items, photoNames)

            return DriveUploader
                .uploadDayBundle(
                    token = token,
                    rootFolderId = DriveExportConfig.ROOT_FOLDER_ID,
                    parentFolderName = DriveExportConfig.PARENT_FOLDER_NAME,
                    dateFolderName = date.toString(),
                    submissionFolderName = "export-${LocalDateTime.now(zone).format(uploadStampFormatter)}",
                    jsonFileName = "$date.json",
                    json = json,
                    photos = photos,
                ).fold(
                    onSuccess = { "$date 내보내기 완료 — $it" },
                    onFailure = { "내보내기 실패: ${it.message}" },
                )
        }

        /** PHOTO 아이템마다 `photos/` 안 파일명을 정한다(순번 접두로 중복 방지). rawId → 파일명. */
        private fun photoFileNames(items: List<SourceItem>): Map<String, String> =
            items
                .filter { it.itemType == ItemType.PHOTO }
                .mapIndexedNotNull { index, item ->
                    val payload = item.payload as? PhotoPayload ?: return@mapIndexedNotNull null
                    val base = payload.fileName.ifBlank { "${item.rawId}.jpg" }
                    item.rawId to "${index.toString().padStart(3, '0')}_$base"
                }.toMap()

        private fun buildPhotoUploads(
            items: List<SourceItem>,
            names: Map<String, String>,
        ): List<DriveUploader.PhotoUpload> =
            items.mapNotNull { item ->
                val payload = item.payload as? PhotoPayload ?: return@mapNotNull null
                val name = names[item.rawId] ?: return@mapNotNull null
                val uri = Uri.parse(payload.clientPhotoUri)
                val bytes =
                    if (DriveExportConfig.DOWNSCALE_PHOTOS) {
                        PhotoDownscaler.downscaleJpeg(
                            context,
                            uri,
                            DriveExportConfig.DOWNSCALE_MAX_DIMENSION,
                            DriveExportConfig.DOWNSCALE_JPEG_QUALITY,
                        )
                    } else {
                        PhotoDownscaler.readOriginal(context, uri)
                    } ?: return@mapNotNull null
                DriveUploader.PhotoUpload(fileName = name, bytes = bytes)
            }
    }
