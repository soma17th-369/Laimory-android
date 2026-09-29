package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.inquiry.InquiryDetail

/** 문의 상세 조회 결과. */
@Immutable
sealed interface InquiryDetailContent {
    data object Loading : InquiryDetailContent

    /** 없거나 내 문의가 아니다. 서버가 둘을 같은 404 로 숨긴다. */
    data object NotFound : InquiryDetailContent

    /** 한 번도 받지 못했다. 다시 시도할 수 있다. */
    data object LoadFailed : InquiryDetailContent

    data class Loaded(
        val detail: InquiryDetail,
    ) : InquiryDetailContent
}
