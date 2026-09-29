package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.inquiry.InquirySummary

/** 내 문의 목록 조회 결과. */
@Immutable
sealed interface InquiryListContent {
    data object Loading : InquiryListContent

    /** 보낸 문의가 없다. 서버는 이때 404 가 아니라 빈 배열을 준다. */
    data object Empty : InquiryListContent

    /** 목록을 한 번도 받지 못했다. 다시 시도할 수 있다. */
    data object LoadFailed : InquiryListContent

    /** 서버 정렬(최신 순, 최대 50건) 그대로다. */
    data class Items(
        val inquiries: List<InquirySummary>,
    ) : InquiryListContent
}
