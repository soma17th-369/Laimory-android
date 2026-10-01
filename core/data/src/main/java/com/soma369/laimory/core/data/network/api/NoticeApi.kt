package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.notice.NoticeListResponse
import retrofit2.Response
import retrofit2.http.GET

/** 공지 공개 조회. **인증이 필요 없다.** 페이지네이션도 상세 조회도 없다 — 각 항목이 원문 주소를 싣는다. */
interface NoticeApi {
    @GET("notices")
    suspend fun getNotices(): Response<ApiResponse<NoticeListResponse>>
}
