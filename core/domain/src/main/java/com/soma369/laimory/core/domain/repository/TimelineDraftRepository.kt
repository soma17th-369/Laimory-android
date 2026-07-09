package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import java.time.LocalDate
import java.time.ZoneId

/** 서버 타임라인 초안 생성(사진 업로드 → 생성 → 상태 조회) 경로. */
interface TimelineDraftRepository {
    /**
     * PHOTO 사진들을 presigned 발급받아 S3 에 순수 바이너리 PUT 으로 업로드하고,
     * 각 사진의 서버 파일명을 입력 순서 그대로 반환한다.
     *
     * presign 요청의 contentType/size 와 실제 PUT 의 Content-Type/Content-Length 가 정확히 같아야
     * S3 가 서명을 통과시킨다(불일치·TTL 만료 시 403). 이 일치 보장은 구현 책임이다.
     *
     * @param clientPhotoUris 업로드할 사진의 `content://` URI 목록
     * @return 업로드된 각 사진의 서버 파일명. [clientPhotoUris] 와 같은 순서·크기.
     */
    suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String>

    /**
     * 하루치 [items] 로 초안 생성 작업을 요청하고 [DraftTaskHandle] 을 받는다.
     *
     * @param uploadedPhotoFilenames PHOTO 아이템의 `rawId` → 업로드된 서버 파일명.
     *   PHOTO payload 의 `filename` 은 이 값으로 채워진다.
     */
    suspend fun createDraft(
        recordDate: LocalDate,
        zone: ZoneId,
        items: List<SourceItem>,
        uploadedPhotoFilenames: Map<String, String>,
    ): DraftTaskHandle

    /** 초안 생성 작업 상태를 조회한다. */
    suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot
}
