package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.notice.NoticeListResponse

interface NoticeRemoteDataSource {
    suspend fun getNotices(): NoticeListResponse
}
