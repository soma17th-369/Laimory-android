package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.notice.NoticeListResponse
import com.soma369.laimory.core.data.model.notice.NoticeResponse
import com.soma369.laimory.core.data.network.api.NoticeApi
import com.soma369.laimory.core.data.network.safeApiCall
import javax.inject.Inject

class NoticeRemoteDataSourceImpl
    @Inject
    constructor(
        private val noticeApi: NoticeApi,
    ) : NoticeRemoteDataSource {
        override suspend fun getNotices(): NoticeListResponse = safeApiCall { noticeApi.getNotices() }

        override suspend fun getNotice(noticeId: Long): NoticeResponse = safeApiCall { noticeApi.getNotice(noticeId) }
    }
