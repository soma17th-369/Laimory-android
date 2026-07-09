package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import java.time.LocalDate
import java.time.ZoneId

/**
 * 하루치 수집 아이템으로 서버 초안 생성을 요청한다.
 *
 * PHOTO 아이템이 있으면 먼저 S3 업로드로 서버 파일명을 확보한 뒤, 그 파일명을 draft 의
 * PHOTO payload 에 실어 생성 요청을 보낸다. 업로드/발급/생성 실패는 [BaseUseCase] 를 통해
 * `Result.failure` 로 정규화된다(사진 하나라도 실패하면 전체 중단).
 */
class CreateTimelineDraftUseCase(
    private val repository: TimelineDraftRepository,
    messageHelper: MessageHelper,
) : BaseUseCase(messageHelper) {
    suspend operator fun invoke(
        recordDate: LocalDate,
        zone: ZoneId,
        items: List<SourceItem>,
    ): Result<DraftTaskHandle> =
        execute {
            val photoItems = items.filter { it.itemType == ItemType.PHOTO }
            val uploadedByRawId =
                if (photoItems.isEmpty()) {
                    emptyMap()
                } else {
                    val uris = photoItems.map { (it.payload as PhotoPayload).clientPhotoUri }
                    val filenames = repository.uploadPhotos(uris)
                    photoItems.mapIndexed { index, item -> item.rawId to filenames[index] }.toMap()
                }
            repository.createDraft(recordDate, zone, items, uploadedByRawId)
        }
}
