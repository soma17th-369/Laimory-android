package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
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
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            items: List<SourceItem>,
        ): Result<DraftTaskHandle> =
            execute {
                // 기록 창이 요약과 서버 요청의 단일 기준이다. 호출 화면이 실수로 전체 목록을 넘겨도
                // 업로드와 요청 직전에 같은 창으로 다시 제한해 구간 밖 데이터가 전송되지 않게 한다.
                // 서버에는 시간순(오래된→최신)으로 보낸다. 로컬 조회(observeAll)는 표시용 최신순이라
                // 그 순서가 그대로 전송되지 않도록 전송 직전에 startAt 오름차순으로 재정렬한다.
                val ordered = items.filter(window::contains).sortedBy { it.startAt }
                val photoItems = ordered.filter { it.itemType == ItemType.PHOTO }
                val uploadedByRawId =
                    if (photoItems.isEmpty()) {
                        emptyMap()
                    } else {
                        val uris = photoItems.map { (it.payload as PhotoPayload).clientPhotoUri }
                        val filenames = repository.uploadPhotos(uris)
                        photoItems.mapIndexed { index, item -> item.rawId to filenames[index] }.toMap()
                    }
                repository.createDraft(recordDate, zone, window, ordered, uploadedByRawId)
            }
    }
