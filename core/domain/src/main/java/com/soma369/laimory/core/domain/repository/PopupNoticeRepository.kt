package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.notice.Notice

/**
 * 앱 시작 팝업 공지 — 관리자가 팝업으로 지정한 공지와 이 기기에서 이미 띄운 기록.
 *
 * 띄운 기록은 계정이 아니라 **기기 단위**다. 공지는 계정과 무관한 공개 글이라, 다른 계정으로 들어와도 이 기기에서
 * 이미 본 팝업을 다시 띄울 이유가 없다(설정 공지 읽음 기록과 같은 이유).
 */
interface PopupNoticeRepository {
    /** 팝업으로 지정되고 숨김이 아닌 공지 id, 최신 순. 앱 초기화 응답에 실린다(로그인 필요). */
    suspend fun getPopupNoticeIds(): List<Long>

    /** 공지 한 건. 숨겨졌거나 없으면 404 로 실패한다(인증 불필요). */
    suspend fun getNotice(noticeId: Long): Notice

    /** 이 기기에서 이미 띄워 닫은 팝업 공지 id. */
    suspend fun getSeenIds(): Set<Long>

    /**
     * [noticeId] 를 띄워 닫았다고 남긴다.
     *
     * 서버 목록에서 빠진 id 를 지우지 않는다 — 숨겼다 다시 노출하면 같은 id 로 돌아오는데, 지웠다면 이미 본 사람에게
     * 또 뜬다. 대신 id 가 큰 순으로 일정 개수만 남긴다.
     */
    suspend fun markSeen(noticeId: Long)
}
