package com.soma369.laimory.core.data.network.api

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.notice.NoticeListResponse
import com.soma369.laimory.core.data.model.notice.NoticeResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/** 공지 공개 조회. **인증이 필요 없다.** 페이지네이션은 없다 — 각 항목이 원문 주소를 싣는다. */
interface NoticeApi {
    @GET("notices")
    suspend fun getNotices(): Response<ApiResponse<NoticeListResponse>>

    /** 공지 한 건. 목록 원소와 같은 모양이다. 숨겼거나 없으면 404(`-404`) — 앱 시작 팝업이 id 로 내용을 받는 경로다. */
    @GET("notices/{noticeId}")
    suspend fun getNotice(
        @Path("noticeId") noticeId: Long,
    ): Response<ApiResponse<NoticeResponse>>
}
