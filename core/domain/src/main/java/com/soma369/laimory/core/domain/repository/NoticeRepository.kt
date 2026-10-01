package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.notice.Notice

/**
 * 공지 조회와 이 기기의 읽음 기록.
 *
 * 앱은 공지를 읽기만 한다 — 등록·수정·숨김은 관리자 웹이 갖는다. 읽음 기록은 서버에 없고 기기에만
 * 둔다(공지 조회가 인증 없는 공개 경로라 사람 단위로 기록할 곳이 없다).
 */
interface NoticeRepository {
    /** 노출 중인 공지 전부, 최신 순. 없으면 빈 목록이다(오류가 아니다). */
    suspend fun getNotices(): List<Notice>

    /** 이 기기에서 눌러 연 공지 id. */
    suspend fun getReadNoticeIds(): Set<Long>

    /**
     * [noticeId] 를 읽음으로 남긴다.
     *
     * [keepIds] 밖의 기록은 이때 버린다 — 새 공지 표시 기간이 지난 공지의 읽음 여부는 더 이상
     * 쓰이지 않으므로, 쌓아 둘 이유가 없다.
     */
    suspend fun markRead(
        noticeId: Long,
        keepIds: Set<Long>,
    )
}
