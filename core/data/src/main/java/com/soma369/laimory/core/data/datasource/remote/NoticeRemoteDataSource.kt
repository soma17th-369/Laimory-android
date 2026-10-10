package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.notice.NoticeListResponse
import com.soma369.laimory.core.data.model.notice.NoticeResponse

interface NoticeRemoteDataSource {
    suspend fun getNotices(): NoticeListResponse

    suspend fun getNotice(noticeId: Long): NoticeResponse
}
