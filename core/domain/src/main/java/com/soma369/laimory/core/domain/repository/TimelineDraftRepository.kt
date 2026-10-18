package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
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
     * 선택한 [window] 안의 [items] 로 초안 생성 작업을 요청하고 [DraftTaskHandle] 을 받는다.
     *
     * @param uploadedPhotoFilenames PHOTO 아이템의 `rawId` → 업로드된 서버 파일명.
     *   PHOTO payload 의 `filename` 은 이 값으로 채워진다.
     */
    suspend fun createDraft(
        recordDate: LocalDate,
        zone: ZoneId,
        window: RecordDateWindow,
        items: List<SourceItem>,
        uploadedPhotoFilenames: Map<String, String>,
    ): DraftTaskHandle

    /** 초안 생성 작업 상태를 조회한다. */
    suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot

    /**
     * [items] 로 타임라인을 만들 수 있는지 서버에 묻는다. 기준은 서버가 갖는다(기록이 너무 적은 날은 false).
     *
     * 판정에 쓰는 종류·시각만 보내고 payload 는 보내지 않는다. 생성 요청은 이 판정을 강제하지 않는다 — 버튼을 끌지 정하는 힌트다.
     */
    suspend fun isCreatable(items: List<SourceItem>): Boolean
}
