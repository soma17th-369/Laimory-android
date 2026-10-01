package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.notice.Notice

/** 공지 목록 조회 결과. */
@Immutable
sealed interface NoticeListContent {
    data object Loading : NoticeListContent

    /** 노출 중인 공지가 없다. 서버는 이때 404 가 아니라 빈 배열을 준다. */
    data object Empty : NoticeListContent

    /** 목록을 한 번도 받지 못했다. 다시 시도할 수 있다. */
    data object LoadFailed : NoticeListContent

    /**
     * @param notices 서버 정렬(최신 순) 그대로다.
     * @param newIds 새 공지 표시를 달 공지. 누르면 빠진다.
     */
    data class Items(
        val notices: List<Notice>,
        val newIds: Set<Long> = emptySet(),
    ) : NoticeListContent
}
