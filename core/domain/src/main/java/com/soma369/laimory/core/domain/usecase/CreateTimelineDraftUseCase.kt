package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReporter
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 선택한 기록 창 안의 수집 아이템으로 서버 초안 생성을 요청한다.
 *
 * 기록 창 필터와 타입별·전체 상한을 적용한 뒤 선택된 항목만 서버에 전달한다.
 * 사용자 선택 PHOTO가 상한을 초과하면 자동 절삭하지 않고 실패한다.
 * PHOTO 아이템이 있으면 먼저 S3 업로드로 서버 파일명을 확보한 뒤, 그 파일명을 draft 의
 * PHOTO payload 에 실어 생성 요청을 보낸다. 업로드/발급/생성 실패는 [BaseUseCase] 를 통해
 * `Result.failure` 로 정규화된다(사진 하나라도 실패하면 전체 중단).
 */
@Singleton
class CreateTimelineDraftUseCase
    @Inject
    constructor(
        private val repository: TimelineDraftRepository,
        messageHelper: MessageHelper,
        private val selectionPolicy: DraftSourceItemSelectionPolicy = DraftSourceItemSelectionPolicy(),
        private val selectionReporter: DraftSourceItemSelectionReporter = DraftSourceItemSelectionReporter.NONE,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            items: List<SourceItem>,
        ): Result<DraftTaskHandle> {
            val selection =
                runCatching {
                    selectionPolicy.select(window, items).getOrThrow().also { selected ->
                        if (selectionReporter.isEnabled) selectionReporter.reportSelection(selected.report)
                    }
                }.getOrElse { return Result.failure(it) }

            return execute {
                val selectedItems = selection.items
                val photoItems = selectedItems.filter { it.itemType == ItemType.PHOTO }
                val uploadedByRawId =
                    if (photoItems.isEmpty()) {
                        emptyMap()
                    } else {
                        val uris = photoItems.map { (it.payload as PhotoPayload).clientPhotoUri }
                        val filenames = repository.uploadPhotos(uris)
                        photoItems.mapIndexed { index, item -> item.rawId to filenames[index] }.toMap()
                    }
                repository.createDraft(recordDate, zone, window, selectedItems, uploadedByRawId)
            }
        }
    }
